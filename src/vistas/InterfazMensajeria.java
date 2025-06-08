package vistas;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.FloatControl;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.text.*;
import controlador.Controlador;
import mensajeria.*;
import persistencia.MensajeDAO;
import persistencia.MensajeDAOFactory;

import java.awt.*;
import java.awt.event.*;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import cifrado.CifradoStrategy;
import cifrado.AesStrategy;
import cifrado.DesStrategy;
import cifrado.TripleDesStrategy;
@SuppressWarnings("serial")
public class InterfazMensajeria extends JFrame implements InterfazVista {
    private DefaultListModel<String> modeloContactos;
    private JList<String> listaContactos;
    private JTextPane areaMensajes;
    private JTextArea areaTextoMensaje;
    private JButton btnAgregarContacto;
    private JButton botonEnviar;
    private Controlador controlador;
    // Abstract Factory:
    private MensajeDAO mensajeDAO;

    private static final String FUENTE = "Segoe UI";
    private static final Color COLOR_BG_VENTANA = new Color(34, 44, 54);
    private static final Color COLOR_BG_CONTACTOS = new Color(28, 38, 48);
    private static final Color COLOR_BURBUJA_MIO = new Color(42, 117, 89);
    private static final Color COLOR_BURBUJA_OTRO = new Color(49, 58, 72);
    private static final Color COLOR_TXT = new Color(255, 255, 255);
    private final CifradoStrategy cifradoStrategy;      // MODIFICAR PARA CAMBIAR EL ALGORITMO DE CIFRADO

    public InterfazMensajeria(Usuario usuario, String algoritmo, String claveUsuario) {
    	this.cifradoStrategy = crearCifradoStrategy(algoritmo, claveUsuario);
        setTitle(usuario.getNickname() + " - Puerto " + usuario.getPuerto());
        setSize(600, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // -------- Panel de contactos --------
        JPanel panelContactos = new JPanel(new BorderLayout());
        panelContactos.setBackground(COLOR_BG_CONTACTOS);
        panelContactos.setBorder(new LineBorder(Color.BLACK, 2));

        // --------- INICIALIZACIÓN MENSAJE DAO ---------
        int tipo = usuario.getTipoAlmacenamiento();
        String extension = tipo == 1 ? ".xml" : tipo == 2 ? ".json" : ".txt";
        String nombreArchivo = usuario.getNickname() + extension;
        this.mensajeDAO = MensajeDAOFactory.getFactory(tipo).crearMensajeDAO(nombreArchivo);
        // ------------------------------------------------
        modeloContactos = new DefaultListModel<>();
        try {
            List<Contacto> agenda = mensajeDAO.cargarContactos();
            for (Contacto c : agenda) usuario.agregarContacto(c);

            modeloContactos.clear();
            for (Contacto c : usuario.getAgenda().values()) {
                modeloContactos.addElement(c.getNombre());
            }
            for (Contacto c : usuario.getAgenda().values()) {
                if (usuario.buscaChat(c.getNombre()) == null) {
                    usuario.agregarChat(new Chat(c));
                }
            }
        } catch (Exception e) {
            showErrorDialog("No se pudo cargar la agenda: " + e.getMessage());
        }
        listaContactos = new JList<>(modeloContactos);
        listaContactos.setBackground(COLOR_BG_CONTACTOS);
        listaContactos.setForeground(COLOR_TXT);
        listaContactos.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setForeground(COLOR_TXT);
                label.setBackground(isSelected ? new Color(52, 72, 100) : COLOR_BG_CONTACTOS);
                label.setFont(new Font(FUENTE, Font.PLAIN, 15));
                label.setOpaque(true);
                return label;
            }
        });
        panelContactos.add(new JScrollPane(listaContactos), BorderLayout.CENTER);

        btnAgregarContacto = new JButton("Agregar Contacto");
        btnAgregarContacto.setActionCommand(ABRIRVENTAGREGARCONTACTO);
        btnAgregarContacto.setFont(new Font(FUENTE, Font.BOLD, 13));
        btnAgregarContacto.setBackground(new Color(80, 140, 200));
        btnAgregarContacto.setForeground(Color.WHITE);
        btnAgregarContacto.setFocusPainted(false);

        JPanel panelBtnAgregar = new JPanel(new BorderLayout());
        panelBtnAgregar.setBackground(COLOR_BG_CONTACTOS);
        panelBtnAgregar.setBorder(BorderFactory.createEmptyBorder(2, 8, 8, 8));
        panelBtnAgregar.add(btnAgregarContacto, BorderLayout.SOUTH);
        panelContactos.add(panelBtnAgregar, BorderLayout.SOUTH);

        // -------- Mensajes --------
        JPanel panelMensajes = new JPanel(new BorderLayout());
        panelMensajes.setBackground(COLOR_BG_VENTANA);
        panelMensajes.setBorder(new LineBorder(Color.BLACK, 2));

        areaMensajes = new JTextPane();
        areaMensajes.setEditable(false);
        areaMensajes.setBackground(COLOR_BG_VENTANA);
        areaMensajes.setFont(new Font(FUENTE, Font.PLAIN, 16));
        areaMensajes.setForeground(COLOR_TXT);
        areaMensajes.setMargin(new Insets(9, 9, 9, 9));
        JScrollPane scrollMensajes = new JScrollPane(areaMensajes);
        scrollMensajes.getViewport().setBackground(COLOR_BG_VENTANA);
        panelMensajes.add(scrollMensajes, BorderLayout.CENTER);

        areaTextoMensaje = new JTextArea(3, 20);
        areaTextoMensaje.setBackground(new Color(36, 46, 58));
        areaTextoMensaje.setCaretColor(COLOR_TXT);
        areaTextoMensaje.setForeground(COLOR_TXT);
        areaTextoMensaje.setFont(new Font(FUENTE, Font.PLAIN, 15));
        areaTextoMensaje.setBorder(new LineBorder(new Color(65, 80, 100), 1));
        areaTextoMensaje.setLineWrap(true);
        areaTextoMensaje.setWrapStyleWord(true);

        JPanel panelInput = new JPanel(new BorderLayout());
        panelInput.setBackground(COLOR_BG_VENTANA);
        panelInput.setBorder(new LineBorder(Color.BLACK, 2));
        panelInput.add(areaTextoMensaje, BorderLayout.CENTER);

        botonEnviar = new JButton("Enviar");
        botonEnviar.setPreferredSize(new Dimension(80, 36));
        botonEnviar.setActionCommand(ENVIARMENSAJE);
        botonEnviar.setFont(new Font(FUENTE, Font.BOLD, 14));
        botonEnviar.setBackground(new Color(52, 122, 70));
        botonEnviar.setForeground(Color.WHITE);
        botonEnviar.setFocusPainted(false);

        panelInput.add(botonEnviar, BorderLayout.EAST);
        panelMensajes.add(panelInput, BorderLayout.SOUTH);

        add(panelContactos, BorderLayout.WEST);
        add(panelMensajes, BorderLayout.CENTER);

        getContentPane().setBackground(COLOR_BG_VENTANA);

        // ENTER envía mensaje
        areaTextoMensaje.getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enviarMensaje"
        );
        areaTextoMensaje.getActionMap().put("enviarMensaje", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override
            public void actionPerformed(ActionEvent e) {
                if (controlador != null) {
                    controlador.actionPerformed(new ActionEvent(
                        botonEnviar, ActionEvent.ACTION_PERFORMED, ENVIARMENSAJE
                    ));
                }
            }
        });

        // -------- SELECCIONAR CONTACTOS Y FOCUS AUTOMÁTICO AREA MENSAJE --------
        listaContactos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                seleccionarContactoYFocusMensaje(usuario);
            }
        });

        listaContactos.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER ||
                        e.getKeyCode() == KeyEvent.VK_SPACE ||
                        e.getKeyCode() == KeyEvent.VK_UP ||
                        e.getKeyCode() == KeyEvent.VK_DOWN
                ) {
                    seleccionarContactoYFocusMensaje(usuario);
                }
            }
        });

        setVisible(true);
    }

    // Cargar y mostrar mensajes del chat al abrir contacto
    private void seleccionarContactoYFocusMensaje(Usuario usuario) {
        String seleccionado = listaContactos.getSelectedValue();
        if (seleccionado != null) {
            String nombreLimpio = seleccionado.replace(" (nuevo)", "");
            if (seleccionado.endsWith(" (nuevo)")) {
                int index = modeloContactos.indexOf(seleccionado);
                if (index != -1) {
                    modeloContactos.set(index, nombreLimpio);
                }
            }
            // ----<<< PERSISTENCIA: CARGAR MENSAJES >>>---
            Chat chat = usuario.buscaChat(nombreLimpio);
            areaMensajes.setText("");
            if (chat != null) {
                // SOLO cargar de disco si el chat está vacío (nunca lo han abierto en esta sesión)
                if (chat.getMensajes().isEmpty()) {
                    try {
                        List<Mensaje> mensajes = mensajeDAO.cargarMensajes(chat.getContacto().getNombre());
                        chat.setMensajes(mensajes); // actualiza Chat solo vacío
                    } catch (Exception e) {
                        showErrorDialog("No se pudieron cargar los mensajes de " + nombreLimpio + " : " + e.getMessage());
                    }
                }
                // Mostrar SIEMPRE todos los mensajes actualmente en memoria
                for (Mensaje m : chat.getMensajes()) {
                    boolean esPropio = m.getNicknameRemitente().equals(usuario.getNickname());
                    String timestamp = String.format("%02d:%02d", m.getTimestamp().getHours(), m.getTimestamp().getMinutes());
                    String texto = timestamp + "   " + m.getContenido();
                    appendMensaje(texto, esPropio);
                }
            }
            SwingUtilities.invokeLater(() -> areaTextoMensaje.requestFocusInWindow());
        }
    }

    // Maneja el envío e incluye guardado
    private void enviarMensaje(String textoPlano, String contacto, Usuario usuario) {
        try {
            Contacto destino = usuario.buscaContactoPorNombre(contacto);
            if (destino == null) { showErrorDialog("Contacto no encontrado"); return; }

            /* 1. Cifrar */
            String cifrado = cifradoStrategy.cifrar(
                    textoPlano);           // receptor

            /* 2. Armar objeto Mensaje con TEXTO CIFRADO */
            Mensaje m = new Mensaje(
                    cifrado,
                    usuario.getNickname(),
                    usuario.getPuerto(),
                    destino.getDireccionIP(),
                    destino.getPuerto(),
                    destino.getNickname());
            System.out.println(m);
            /* 3. UI y persistencia en CLARO */
            String ts = String.format("%02d:%02d", m.getTimestamp().getHours(), m.getTimestamp().getMinutes());
            appendMensaje(ts + "   " + textoPlano, true);
            areaTextoMensaje.setText("");

            try (Socket s = new Socket("localhost", 10000);
                 ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream())) {
                out.flush();
                out.writeObject(m);
            }

            // Guardamos el plano
            m.setContenido(textoPlano);
            usuario.agregarMensaje(m, contacto);
            Chat chat = usuario.buscaChat(contacto);
            if (chat != null) mensajeDAO.guardarMensajes(chat.getContacto().getNombre(), chat.getMensajes());

        } catch (Exception e) {
            showErrorDialog("Error al enviar: " + e.getMessage());
        }
    }

    public void formarMensaje(Usuario usuario) {
        String contenido = areaTextoMensaje.getText();
        if (!contenido.isEmpty()) {
            String contactoSeleccionado = listaContactos.getSelectedValue();
            if (contactoSeleccionado != null) {
                contactoSeleccionado = contactoSeleccionado.replace(" (nuevo)", "");
                enviarMensaje(contenido, contactoSeleccionado, usuario);
            } else {
                showErrorDialog("Por favor, selecciona un contacto.");
            }
        }
    }


    /************************************************************
     * MÉTODO recibirMensaje – ahora descifra apenas llega
     ************************************************************/
    public void recibirMensaje(Mensaje mensaje,
                               Socket soc,
                               Usuario usuario) {
    	try {
            /* 1. Descifrar al llegar */
            String plano = cifradoStrategy.descifrar(
                    mensaje.getContenido());    // emisor
            mensaje.setContenido(plano);
        } catch (Exception e) {
            showErrorDialog("No se pudo descifrar: probablemente tú y tu contacto usan claves o métodos distintos.\n\n");
            return;
        }
            

        /*------------------------------------------------------
         * 3. ***Todo lo que sigue es tu lógica original***
         *    (agenda, chats, persistencia, UI, notificaciones…)
         *-----------------------------------------------------*/
        String remitente = mensaje.getNicknameRemitente();
        String contactoSeleccionado = listaContactos.getSelectedValue();

        Contacto contacto = usuario.buscaContactoPorNickname(remitente);
        if (contacto == null) {
            String ip     = soc.getInetAddress().getHostAddress();
            int    puerto = mensaje.getPuertoRemitente();
            contacto = new Contacto(remitente, ip, puerto, remitente);
            usuario.agregarContacto(contacto);

            if (modeloContactos.indexOf(contacto.getNombre()) == -1) {
                modeloContactos.addElement(contacto.getNombre());
            }
            try {
                mensajeDAO.guardarContactos(new ArrayList<>(usuario.getAgenda().values()));
            } catch (Exception ex) {
                showErrorDialog("No se pudo guardar la agenda: " + ex.getMessage());
            }
        }

        Chat chat = usuario.buscaChat(contacto.getNombre());
        if (chat == null) {
            chat = new Chat(contacto);
            usuario.agregarChat(chat);
        }

        if (chat.getMensajes().isEmpty()) {
            try {
                List<Mensaje> historicos =
                        mensajeDAO.cargarMensajes(contacto.getNombre());
                chat.setMensajes(historicos);
            } catch (Exception e) {
                showErrorDialog("No se pudieron cargar mensajes históricos: "
                                + e.getMessage());
            }
        }
        usuario.agregarMensaje(mensaje, contacto.getNombre());

        try {
            mensajeDAO.guardarMensajes(chat.getContacto().getNombre(),
                                       chat.getMensajes());
        } catch (Exception ex) {
            showErrorDialog("No se pudo guardar el mensaje recibido: "
                            + ex.getMessage());
        }

        /*------------- Mostrar o notificar en UI -------------*/
        if (contactoSeleccionado != null
                && contactoSeleccionado.replace(" (nuevo)", "")
                                       .equals(contacto.getNombre())) {
            String timestamp = String.format("%02d:%02d",
                                mensaje.getTimestamp().getHours(),
                                mensaje.getTimestamp().getMinutes());
            String textoUI = timestamp + "   " + mensaje.getContenido();
            appendMensaje(textoUI, false);
        } else {
            reproducirSonido();
            boolean marcado = false;
            for (int i = 0; i < modeloContactos.size(); i++) {
                String nombreLista = modeloContactos.get(i);
                if (nombreLista.replace(" (nuevo)", "")
                               .equals(contacto.getNombre())) {
                    if (!nombreLista.contains("(nuevo)")) {
                        modeloContactos.set(i, contacto.getNombre() + " (nuevo)");
                    }
                    marcado = true;
                    break;
                }
            }
            if (!marcado) {
                modeloContactos.addElement(contacto.getNombre() + " (nuevo)");
            }
        }
    }


    // --- Resto de la clase igual que tu versión original, sin cambios ---

    public void abrirVentanaAgregarContacto(Map<String, Usuario> listaNicknames, Usuario usuario) {
        JDialog dialog = new JDialog(this, "Directorio de Usuarios", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(COLOR_BG_VENTANA);

        DefaultListModel<String> modeloListaUsuarios = new DefaultListModel<>();
        for (String nickname : listaNicknames.keySet()) {
            boolean yaAgendado = false;
            for (Contacto c : usuario.getAgenda().values()) {
                if (c.getNickname().equals(nickname)) {
                    yaAgendado = true;
                    break;
                }
            }
            if (!nickname.equals(usuario.getNickname()) && !yaAgendado) {
                modeloListaUsuarios.addElement(nickname);
            }
        }

        JList<String> listaUsuarios = new JList<>(modeloListaUsuarios);
        listaUsuarios.setBackground(COLOR_BG_CONTACTOS);
        listaUsuarios.setSelectionBackground(new Color(52, 100, 100));
        listaUsuarios.setFont(new Font(FUENTE, Font.PLAIN, 15));
        listaUsuarios.setForeground(COLOR_TXT);

        JScrollPane scrollPane = new JScrollPane(listaUsuarios);
        scrollPane.getViewport().setBackground(COLOR_BG_CONTACTOS);

        JButton btnAgendar = new JButton("Agendar Usuario");
        btnAgendar.setFont(new Font(FUENTE, Font.BOLD, 13));
        btnAgendar.setBackground(new Color(80, 140, 200));
        btnAgendar.setForeground(Color.WHITE);
        btnAgendar.setFocusPainted(false);

        btnAgendar.addActionListener(e -> {
            String nicknameSeleccionado = listaUsuarios.getSelectedValue();
            if (nicknameSeleccionado != null) {
                Usuario usuarioSeleccionado = listaNicknames.get(nicknameSeleccionado);

                JDialog inputDialog = new JDialog(dialog, "Nuevo nombre de contacto", true);
                inputDialog.setSize(360, 150);
                inputDialog.setLocationRelativeTo(dialog);

                JPanel panelDialog = new JPanel(new GridBagLayout());
                panelDialog.setBackground(COLOR_BG_VENTANA);
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(8, 12, 8, 12);
                gbc.anchor = GridBagConstraints.WEST;
                gbc.fill = GridBagConstraints.HORIZONTAL;

                JLabel pregunta = new JLabel("Ingrese un nombre para agendar a " + nicknameSeleccionado + ":");
                pregunta.setFont(new Font(FUENTE, Font.PLAIN, 14));
                pregunta.setForeground(COLOR_TXT);
                gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
                panelDialog.add(pregunta, gbc);

                JTextField nombrePersonalizadoFld = new JTextField(nicknameSeleccionado, 18);
                nombrePersonalizadoFld.setBackground(COLOR_BG_CONTACTOS);
                nombrePersonalizadoFld.setBorder(new LineBorder(new Color(65,80,100)));
                nombrePersonalizadoFld.setFont(new Font(FUENTE, Font.PLAIN, 15));
                nombrePersonalizadoFld.setForeground(COLOR_TXT);
                gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
                panelDialog.add(nombrePersonalizadoFld, gbc);

                JButton okBtn = new JButton("OK");
                JButton cancelBtn = new JButton("Cancelar");
                okBtn.setFont(new Font(FUENTE, Font.BOLD, 13));
                okBtn.setBackground(new Color(80, 140, 200));
                okBtn.setForeground(Color.WHITE);
                okBtn.setFocusPainted(false);

                cancelBtn.setFont(new Font(FUENTE, Font.BOLD, 13));
                cancelBtn.setBackground(new Color(120, 120, 120));
                cancelBtn.setForeground(COLOR_TXT);
                cancelBtn.setFocusPainted(false);

                gbc.insets = new Insets(16, 12, 8, 12);
                gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
                panelDialog.add(okBtn, gbc);
                gbc.gridx = 1; gbc.gridy = 2;
                panelDialog.add(cancelBtn, gbc);

                inputDialog.setContentPane(panelDialog);

                okBtn.addActionListener(ev -> {
                    String nombrePersonalizado = nombrePersonalizadoFld.getText();
                    if (nombrePersonalizado != null && !nombrePersonalizado.trim().isEmpty()) {
                        Contacto existentePorNombre = usuario.buscaContactoPorNombre(nombrePersonalizado.trim());
                        if (existentePorNombre != null) {
                            showErrorDialog("Ya existe un contacto con ese nombre.");
                            return;
                        }
                        boolean existePorIpYPuerto = false;
                        for (Contacto c : usuario.getAgenda().values()) {
                            if (c.getDireccionIP().equals(usuarioSeleccionado.getIp())
                                    && c.getPuerto() == usuarioSeleccionado.getPuerto()) {
                                existePorIpYPuerto = true;
                                break;
                            }
                        }
                        if (existePorIpYPuerto) {
                            showErrorDialog("Ya existe un contacto con esa IP y puerto.");
                            return;
                        }
                        Contacto nuevoContacto = new Contacto(
                            nombrePersonalizado.trim(),
                            usuarioSeleccionado.getIp(),
                            usuarioSeleccionado.getPuerto(),
                            usuarioSeleccionado.getNickname()
                        );
                        Chat nuevoChat = new Chat(nuevoContacto);
                        usuario.agregarChat(nuevoChat);
                        usuario.agregarContacto(nuevoContacto);
                        modeloContactos.addElement(nombrePersonalizado.trim());
                        listaContactos.setSelectedValue(nombrePersonalizado.trim(), true);
                        areaTextoMensaje.requestFocusInWindow();
                        try {
                            mensajeDAO.guardarContactos(new ArrayList<>(usuario.getAgenda().values()));
                        } catch (Exception ex) {
                            showErrorDialog("No se pudo guardar la agenda: " + ex.getMessage());
                        }
                        inputDialog.dispose();
                        dialog.dispose();
                    } else {
                        showErrorDialog("Debe ingresar un nombre válido.");
                    }
                });

                cancelBtn.addActionListener(ev -> inputDialog.dispose());

                inputDialog.setUndecorated(false);
                inputDialog.setResizable(false);
                inputDialog.setModal(true);
                inputDialog.setVisible(true);

            } else {
                showErrorDialog("Debe seleccionar un usuario de la lista.");
            }
        });

        JPanel panelBoton = new JPanel(new BorderLayout());
        panelBoton.setBackground(COLOR_BG_VENTANA);
        panelBoton.add(btnAgendar, BorderLayout.SOUTH);

        contentPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.add(panelBoton, BorderLayout.SOUTH);

        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.setVisible(true);
    }
    public JTextPane getAreaMensajes() { return areaMensajes; }
    public void reproducirSonido() {
        try {
            URL sonidoURL = getClass().getClassLoader().getResource("sonido.wav");
            if (sonidoURL == null) {
                System.out.println("Archivo de sonido no encontrado");
                return;
            }
            AudioInputStream audioStream = javax.sound.sampled.AudioSystem.getAudioInputStream(sonidoURL);
            javax.sound.sampled.Clip clip = javax.sound.sampled.AudioSystem.getClip();
            clip.open(audioStream);
            FloatControl volume = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            volume.setValue(-15.0f);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void setAreaMensajes(JTextPane areaMensajes) { this.areaMensajes = areaMensajes; }
    public void setControlador(Controlador c) {
        this.controlador = c;
        botonEnviar.addActionListener(c);
        btnAgregarContacto.addActionListener(c);
    }
    private void showErrorDialog(String msg) {
        JDialog dialog = new JDialog(this, "Error", true);
        dialog.setUndecorated(false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(COLOR_BG_VENTANA);

        JTextArea label = new JTextArea(msg);
        label.setEditable(false);
        label.setForeground(COLOR_TXT);
        label.setBackground(COLOR_BG_VENTANA);
        label.setFont(new Font(FUENTE, Font.PLAIN, 15));
        label.setWrapStyleWord(true);
        label.setLineWrap(true);
        label.setBorder(BorderFactory.createEmptyBorder(18, 24, 10, 24));
        label.setFocusable(false);

        // Si el mensaje puede ser muy largo, emplea JScrollPane.
        JScrollPane scroll = new JScrollPane(label);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setBackground(COLOR_BG_VENTANA);
        scroll.getViewport().setBackground(COLOR_BG_VENTANA);

        contentPanel.add(scroll, BorderLayout.CENTER);

        JButton cerrar = new JButton("Cerrar");
        cerrar.setBackground(new Color(80, 140, 200));
        cerrar.setForeground(Color.WHITE);
        cerrar.setFont(new Font(FUENTE, Font.BOLD, 13));
        cerrar.setFocusPainted(false);
        cerrar.setBorder(BorderFactory.createEmptyBorder(7, 18, 7, 18));
        cerrar.addActionListener(e -> dialog.dispose());

        JPanel panelBoton = new JPanel();
        panelBoton.setBackground(COLOR_BG_VENTANA);
        panelBoton.add(cerrar);

        contentPanel.add(panelBoton, BorderLayout.SOUTH);

        dialog.setContentPane(contentPanel);

        // Tamaño preferido: ancho y alto cómodos (puedes ajustar a tu gusto)
        int ancho = 460;
        int alto = 220;
        dialog.setSize(ancho, alto);

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private CifradoStrategy crearCifradoStrategy(String algoritmo, String claveUsuario) {
        switch(algoritmo) {
            case "AES":
    		try {
    			return new AesStrategy(claveUsuario);
    		} catch (Exception e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
            case "DES":
    		try {
    			return new DesStrategy(claveUsuario);
    		} catch (Exception e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
            case "Triple DES":
    		try {
    			return new TripleDesStrategy(claveUsuario);
    		} catch (Exception e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
            default:
                throw new IllegalArgumentException("Algoritmo no soportado: " + algoritmo);
        }
    }
    private void appendMensaje(String texto, boolean derecha) {
        StyledDocument doc = areaMensajes.getStyledDocument();
        SimpleAttributeSet set = new SimpleAttributeSet();
        StyleConstants.setSpaceAbove(set, 6f);
        StyleConstants.setSpaceBelow(set, 6f);
        StyleConstants.setLeftIndent(set, derecha ? 80f : 8f);
        StyleConstants.setRightIndent(set, derecha ? 8f : 80f);
        if (derecha) {
            StyleConstants.setAlignment(set, StyleConstants.ALIGN_RIGHT);
            StyleConstants.setBackground(set, COLOR_BURBUJA_MIO);
            StyleConstants.setForeground(set, COLOR_TXT);
        } else {
            StyleConstants.setAlignment(set, StyleConstants.ALIGN_LEFT);
            StyleConstants.setBackground(set, COLOR_BURBUJA_OTRO);
            StyleConstants.setForeground(set, Color.WHITE);
        }
        StyleConstants.setFontSize(set, 16);
        StyleConstants.setFontFamily(set, FUENTE);
        try {
            int len = doc.getLength();
            doc.insertString(len, texto + "\n", set);
            doc.setParagraphAttributes(len, texto.length(), set, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }}