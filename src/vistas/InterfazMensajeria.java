package vistas;

import javax.swing.*;
import javax.swing.border.LineBorder;

import mensajeria.Chat;
import mensajeria.Contacto;
import mensajeria.Mensaje;
import mensajeria.Usuario;

import java.awt.*;
import java.awt.event.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URL;

import javax.sound.sampled.*;

public class InterfazMensajeria extends JFrame implements InterfazVista {
    private Usuario usuario;
    private DefaultListModel<String> modeloContactos;
    private JList<String> listaContactos;
    private JTextArea areaMensajes;
    private JTextArea areaTextoMensaje;
    private JTextField campoPuerto;
    private JButton btnAgregarContacto;
    private JButton btnConfiguracion;
    private JButton botonEnviar;
    private Controlador controlador;

    public InterfazMensajeria(Usuario usuario) {
        this.usuario = usuario;
        this.controlador = controlador;
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
                            areaMensajes.append(m.getNicknameRemitente() + ": " + m.getContenido() + "\n");
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
        setVisible(true);
    }

    public void formarMensaje() {
        String contenido = areaTextoMensaje.getText();
        if (!contenido.isEmpty()) {
            String contactoSeleccionado = listaContactos.getSelectedValue();
            if (contactoSeleccionado != null) {
                contactoSeleccionado = contactoSeleccionado.replace(" (nuevo)", "");
                enviarMensaje(contenido, contactoSeleccionado);
                areaMensajes.append("Yo: " + contenido + "\n");
                areaTextoMensaje.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "Por favor, selecciona un contacto.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void recibirMensaje(Mensaje mensaje, Socket soc) {
        String remitente = mensaje.getNicknameRemitente();
        String contactoSeleccionado = listaContactos.getSelectedValue();

        Contacto contacto = usuario.buscaContacto(remitente);
        if (contacto == null) {
            String ip = soc.getInetAddress().getHostAddress();
            int puerto = mensaje.getPuertoRemitente();
            contacto = new Contacto(remitente, ip, puerto, mensaje.getNicknameRemitente());
            usuario.agregarContacto(contacto);
        }

        Chat chat = usuario.buscaChat(remitente);
        if (chat == null) {
            chat = new Chat(contacto);
            usuario.agregarChat(chat);
        }

        usuario.agregarMensaje(mensaje, remitente);

        if (contactoSeleccionado != null && contactoSeleccionado.replace(" (nuevo)", "").equals(remitente)) {
            areaMensajes.append(remitente + ": " + mensaje.getContenido() + "\n");
            
        } else {
        	reproducirSonido(); // Usá la ruta relativa dentro del .jar
            boolean encontrado = false;
            for (int i = 0; i < modeloContactos.size(); i++) {
                String nombreLista = modeloContactos.get(i);
                if (nombreLista.startsWith(remitente)) {
                    if (!nombreLista.contains("(nuevo)")) {
                        modeloContactos.set(i, remitente + " (nuevo)");
                    }
                    encontrado = true;
                    break;
                }
            }
            if (!encontrado) {
                modeloContactos.addElement(remitente + " (nuevo)");
            }
        }
    }

    private void enviarMensaje(String contenidoMensaje, String contacto) {
        try {
            Contacto contactoDestino = usuario.buscaContacto(contacto);
            Mensaje mensaje = new Mensaje(contenidoMensaje, usuario.getNickname(), usuario.getPuerto(), contactoDestino.getDireccionIP(), contactoDestino.getPuerto(), contactoDestino.getNickname());
            Socket socket = new Socket("localhost", 10000);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            out.writeObject(mensaje);
            out.close();
            socket.close();
            usuario.agregarMensaje(mensaje, contacto);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al enviar el mensaje: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void abrirVentanaAgregarContacto() {
        JDialog dialog = new JDialog(this, "Nuevo Contacto", true);
        dialog.setSize(400, 220);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel panelCampos = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        JLabel labelNombre = new JLabel("Nombre / Alias:");
        JTextField campoNombre = new JTextField(20);
        JLabel labelPuerto = new JLabel("Número de Puerto:");
        JTextField campoPuerto = new JTextField(20);
        JLabel labelIP = new JLabel("Dirección IP:");
        JTextField campoIP = new JTextField(20);

        panelCampos.add(labelNombre, gbc);
        gbc.gridx = 1;
        panelCampos.add(campoNombre, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panelCampos.add(labelPuerto, gbc);
        gbc.gridx = 1;
        panelCampos.add(campoPuerto, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panelCampos.add(labelIP, gbc);
        gbc.gridx = 1;
        panelCampos.add(campoIP, gbc);

        JButton btnAgregar = new JButton("Agregar Contacto");
        btnAgregar.addActionListener(e -> {
            String nombre = campoNombre.getText();
            String puerto = campoPuerto.getText();
            String ip = campoIP.getText();

            if (!nombre.isEmpty() && !puerto.isEmpty() && !ip.isEmpty()) {
                try {
                    int puertoInt = Integer.parseInt(puerto);
                 // Verificar si el contacto ya existe por nombre
                    Contacto existentePorNombre = usuario.buscaContacto(nombre);
                    if (existentePorNombre != null) {
                        JOptionPane.showMessageDialog(dialog,
                            "Ya existe un contacto con ese nombre.",
                            "Contacto duplicado",
                            JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    // Verificar si ya existe un contacto con esa IP y puerto
                    boolean existePorIpYPuerto = false;
                    for (Contacto c : usuario.getAgenda().values()) {
                        if (c.getDireccionIP().equals(ip) && c.getPuerto() == puertoInt) {
                            existePorIpYPuerto = true;
                            break;
                        }
                    }

                    if (existePorIpYPuerto) {
                        JOptionPane.showMessageDialog(dialog,
                            "Ya existe un contacto con esa IP y puerto.",
                            "Contacto duplicado",
                            JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    Contacto nuevoContacto = new Contacto(nombre, ip, puertoInt, nombre);   // CORREGIR NOMBRE DEL DIRECTORIO
                    Chat nuevoChat = new Chat(nuevoContacto);
                    usuario.agregarChat(nuevoChat);
                    usuario.agregarContacto(nuevoContacto);
                    modeloContactos.addElement(nombre);
                    dialog.dispose();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(dialog, "El puerto debe ser un número entero.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(dialog, "Por favor, complete todos los campos.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelBoton.add(btnAgregar);

        dialog.add(panelCampos, BorderLayout.CENTER);
        dialog.add(panelBoton, BorderLayout.SOUTH);
        dialog.setResizable(false);
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
            volume.setValue(-6.0f);

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
