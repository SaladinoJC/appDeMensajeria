package vistas;

import javax.swing.*;
import javax.swing.border.LineBorder;

import controlador.Controlador;

import javax.sound.sampled.*;

import mensajeria.Chat;
import mensajeria.Contacto;
import mensajeria.Mensaje;
import mensajeria.Usuario;


import java.awt.*;
import java.awt.event.*;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.Map;

@SuppressWarnings("serial")
public class InterfazMensajeria extends JFrame implements InterfazVista {
    private DefaultListModel<String> modeloContactos;
    private JList<String> listaContactos;
    private JTextArea areaMensajes;
    private JTextArea areaTextoMensaje;
    private JButton btnAgregarContacto;
    private JButton botonEnviar;
    private Controlador controlador;

    public InterfazMensajeria(Usuario usuario) {
        setTitle(usuario.getNickname() + " - Puerto " + usuario.getPuerto());
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Panel de contactos
        JPanel panelContactos = new JPanel(new BorderLayout());
        panelContactos.setBackground(Color.DARK_GRAY);
        panelContactos.setBorder(new LineBorder(Color.BLACK, 2));

        modeloContactos = new DefaultListModel<>();
        listaContactos = new JList<>(modeloContactos);
        listaContactos.setBackground(Color.LIGHT_GRAY);
        panelContactos.add(new JScrollPane(listaContactos), BorderLayout.CENTER);

        // Click en contacto → mostrar mensajes
        listaContactos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String seleccionado = listaContactos.getSelectedValue();
                if (seleccionado != null) {
                    String nombreLimpio = seleccionado.replace(" (nuevo)", "");
                    // Limpiar el (nuevo) del modelo visual si está presente
                    if (seleccionado.endsWith(" (nuevo)")) {
                        int index = modeloContactos.indexOf(seleccionado);
                        if (index != -1) {
                            modeloContactos.set(index, nombreLimpio);
                        }
                    }
                    Chat chat = usuario.buscaChat(nombreLimpio);
                    areaMensajes.setText("");
                    if (chat != null) {
                        for (Mensaje m : chat.getMensajes()) {
                            // Buscar el contacto correspondiente para saber el nombre personalizado
                            Contacto c = usuario.buscaContactoPorNombre(m.getNicknameRemitente());
                            String nombreParaMostrar = (c != null) ? c.getNombre() : m.getNicknameRemitente();
                            areaMensajes.append(nombreParaMostrar +": " + m.getContenido() + "  " + m.getTimestamp().getHours() +":"+ m.getTimestamp().getMinutes() + "\n");
                        }
                    }
                }
            }
        });

        // Panel botones
        JPanel panelBotones = new JPanel(new GridLayout(2, 1));
        panelBotones.setBackground(Color.DARK_GRAY);
        panelBotones.setBorder(new LineBorder(Color.BLACK, 2));

        btnAgregarContacto = new JButton("Agregar Contacto");
        btnAgregarContacto.setActionCommand(ABRIRVENTAGREGARCONTACTO);
        panelBotones.add(btnAgregarContacto);

        panelContactos.add(panelBotones, BorderLayout.SOUTH);

        // Panel mensajes
        JPanel panelMensajes = new JPanel(new BorderLayout());
        panelMensajes.setBackground(Color.DARK_GRAY);
        panelMensajes.setBorder(new LineBorder(Color.BLACK, 2));

        areaMensajes = new JTextArea();
        areaMensajes.setEditable(false);
        areaMensajes.setBackground(Color.LIGHT_GRAY);
        panelMensajes.add(new JScrollPane(areaMensajes), BorderLayout.CENTER);

        areaTextoMensaje = new JTextArea(3, 20);
        areaTextoMensaje.setBackground(Color.LIGHT_GRAY);

        JPanel panelInput = new JPanel(new BorderLayout());
        panelInput.setBackground(Color.DARK_GRAY);
        panelInput.setBorder(new LineBorder(Color.BLACK, 2));
        panelInput.add(areaTextoMensaje, BorderLayout.CENTER);

        botonEnviar = new JButton("Enviar");
        botonEnviar.setPreferredSize(new Dimension(80, 30));
        botonEnviar.setActionCommand(ENVIARMENSAJE);
        panelInput.add(botonEnviar, BorderLayout.EAST);

        panelMensajes.add(panelInput, BorderLayout.SOUTH);

        // Agregar paneles a la ventana principal
        add(panelContactos, BorderLayout.WEST);
        add(panelMensajes, BorderLayout.CENTER);

        getContentPane().setBackground(Color.DARK_GRAY);

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

        setVisible(true);
    }

    public void formarMensaje(Usuario usuario) {
        String contenido = areaTextoMensaje.getText();
        if (!contenido.isEmpty()) {
            String contactoSeleccionado = listaContactos.getSelectedValue();
            if (contactoSeleccionado != null) {
                contactoSeleccionado = contactoSeleccionado.replace(" (nuevo)", "");
                enviarMensaje(contenido, contactoSeleccionado, usuario);
            } else {
                JOptionPane.showMessageDialog(
                    this, "Por favor, selecciona un contacto.",
                    "Error", JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    public void recibirMensaje(Mensaje mensaje, Socket soc, Usuario usuario) {
        String remitente = mensaje.getNicknameRemitente();
        String contactoSeleccionado = listaContactos.getSelectedValue();
        
        Contacto contacto = usuario.buscaContactoPorNickname(remitente);
        if (contacto == null) {
            String ip = soc.getInetAddress().getHostAddress();
            int puerto = mensaje.getPuertoRemitente();
            contacto = new Contacto(remitente, ip, puerto, mensaje.getNicknameRemitente());
            usuario.agregarContacto(contacto);
        }

        Chat chat = usuario.buscaChat(contacto.getNombre());
        if (chat == null) {
            chat = new Chat(contacto);
            usuario.agregarChat(chat);
        }

        usuario.agregarMensaje(mensaje, contacto.getNombre());

        if (contactoSeleccionado != null && contactoSeleccionado.replace(" (nuevo)", "").equals(contacto.getNombre())) {
        	areaMensajes.append(contacto.getNombre() +": " + mensaje.getContenido() + "  " + mensaje.getTimestamp().getHours() +":"+ mensaje.getTimestamp().getMinutes() + "\n");
        } else {
            reproducirSonido(); // Usar la ruta relativa dentro del .jar
            boolean encontrado = false;
            for (int i = 0; i < modeloContactos.size(); i++) {
                String nombreLista = modeloContactos.get(i);
                if (nombreLista.replace(" (nuevo)", "").equals(contacto.getNombre())) {
                    if (!nombreLista.contains("(nuevo)")) {
                        modeloContactos.set(i, contacto.getNombre() + " (nuevo)");
                    }
                    encontrado = true;
                    break;
                }
            }
            if (!encontrado) {
                modeloContactos.addElement(contacto.getNombre() + " (nuevo)");
            }
        }
    }

    private void enviarMensaje(String contenidoMensaje, String contacto, Usuario usuario) {
        try {
            Contacto contactoDestino = usuario.buscaContactoPorNombre(contacto);
            Mensaje mensaje = new Mensaje(
                contenidoMensaje,
                usuario.getNickname(),
                usuario.getPuerto(),
                contactoDestino.getDireccionIP(),
                contactoDestino.getPuerto(),
                contactoDestino.getNickname()
            );
            
            areaMensajes.append(usuario.getNickname() +": " + contenidoMensaje + "  " + mensaje.getTimestamp().getHours() +":"+ mensaje.getTimestamp().getMinutes() + "\n");
            areaTextoMensaje.setText("");
            
            Socket socket = new Socket("localhost", 10000);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            out.writeObject(mensaje);
            out.close();
            socket.close();
            usuario.agregarMensaje(mensaje, contacto);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                this,
                "Error al enviar el mensaje: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE
            );
        }
    }

    public void abrirVentanaAgregarContacto(Map<String, Usuario> listaNicknames, Usuario usuario) {
        JDialog dialog = new JDialog(this, "Directorio de Usuarios", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

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
        JScrollPane scrollPane = new JScrollPane(listaUsuarios);

        JButton btnAgendar = new JButton("Agendar Usuario");
        btnAgendar.addActionListener(e -> {
            String nicknameSeleccionado = listaUsuarios.getSelectedValue();
            if (nicknameSeleccionado != null) {
                Usuario usuarioSeleccionado = listaNicknames.get(nicknameSeleccionado);

                // Preguntar por un nombre de contacto personalizado
                String nombrePersonalizado = (String) JOptionPane.showInputDialog(
                    dialog,
                    "Ingrese un nombre para agendar a " + nicknameSeleccionado + ":",
                    "Nuevo nombre de contacto",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    nicknameSeleccionado
                );
                if (nombrePersonalizado != null && !nombrePersonalizado.trim().isEmpty()) {
                    // Verificar si ya existe un contacto con ese nombre
                    Contacto existentePorNombre = usuario.buscaContactoPorNombre(nombrePersonalizado.trim());
                    if (existentePorNombre != null) {
                        JOptionPane.showMessageDialog(
                            dialog, "Ya existe un contacto con ese nombre.",
                            "Contacto duplicado", JOptionPane.ERROR_MESSAGE
                        );
                        return;
                    }
                    // Verificar si ya existe un contacto con esa IP y puerto
                    boolean existePorIpYPuerto = false;
                    for (Contacto c : usuario.getAgenda().values()) {
                        if (c.getDireccionIP().equals(usuarioSeleccionado.getIp())
                                && c.getPuerto() == usuarioSeleccionado.getPuerto()) {
                            existePorIpYPuerto = true;
                            break;
                        }
                    }
                    if (existePorIpYPuerto) {
                        JOptionPane.showMessageDialog(
                            dialog, "Ya existe un contacto con esa IP y puerto.",
                            "Contacto duplicado", JOptionPane.ERROR_MESSAGE
                        );
                        return;
                    }
                    // Si todo bien, crear el nuevo contacto
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
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(
                        dialog, "Debe ingresar un nombre válido.", "Error", JOptionPane.ERROR_MESSAGE
                    );
                }
            } else {
                JOptionPane.showMessageDialog(
                    dialog, "Debe seleccionar un usuario de la lista.", "Error", JOptionPane.ERROR_MESSAGE
                );
            }
        });

        JPanel panelBoton = new JPanel();
        panelBoton.add(btnAgendar);

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(panelBoton, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    public JTextArea getAreaMensajes() {
        return areaMensajes;
    }

    public void reproducirSonido() {
        try {
            URL sonidoURL = getClass().getClassLoader().getResource("sonido.wav");
            if (sonidoURL == null) {
                System.out.println("Archivo de sonido no encontrado");
                return;
            }
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(sonidoURL);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            // Volumen al 50% (aproximadamente -6 dB)
            FloatControl volume = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            volume.setValue(-15.0f);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setAreaMensajes(JTextArea areaMensajes) {
        this.areaMensajes = areaMensajes;
    }

    public void setControlador(Controlador c) {
        this.controlador = c;
        botonEnviar.addActionListener(c);
        btnAgregarContacto.addActionListener(c);
    }
}