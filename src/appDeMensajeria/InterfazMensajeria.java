package appDeMensajeria;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;

class InterfazMensajeria extends JFrame implements InterfazVista {
    private UsuarioEmisor usuario;
    private DefaultListModel<String> modeloContactos;
    private JList<String> listaContactos;
    private JTextArea areaMensajes;

	private JTextArea areaTextoMensaje;
    private JTextField campoPuerto;
    private JButton btnAgregarContacto;
    private JButton btnConfiguracion;
    private JButton botonEnviar;
    
    
    public InterfazMensajeria(UsuarioEmisor usuario) {
        this.usuario = usuario;
        setTitle("Sistema de Mensajería Instantánea");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Panel de contactos
        JPanel panelContactos = new JPanel();
        panelContactos.setLayout(new BorderLayout());
        panelContactos.setBackground(Color.DARK_GRAY);
        panelContactos.setBorder(new LineBorder(Color.BLACK, 2)); // Borde negro

        modeloContactos = new DefaultListModel<>();
        listaContactos = new JList<>(modeloContactos);
        listaContactos.setBackground(Color.LIGHT_GRAY);
        panelContactos.add(new JScrollPane(listaContactos), BorderLayout.CENTER);
        
        // Panel para botones de acción
        JPanel panelBotones = new JPanel();
        panelBotones.setLayout(new GridLayout(2, 1));
        panelBotones.setBackground(Color.DARK_GRAY);
        panelBotones.setBorder(new LineBorder(Color.BLACK, 2)); // Borde negro
        
        btnAgregarContacto = new JButton("Agregar Contacto");
        btnAgregarContacto.setActionCommand(ABRIRVENTAGREGARCONTACTO);
        
        /*btnAgregarContacto.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                abrirVentanaAgregarContacto();
            }
        });*/
        
        btnConfiguracion = new JButton("Configuración");
        panelBotones.add(btnAgregarContacto);
        panelBotones.add(btnConfiguracion);
        
        // Agregar panel de botones al panel de contactos
        panelContactos.add(panelBotones, BorderLayout.SOUTH);

        // Panel de mensajes
        JPanel panelMensajes = new JPanel();
        panelMensajes.setLayout(new BorderLayout());
        panelMensajes.setBackground(Color.DARK_GRAY);
        panelMensajes.setBorder(new LineBorder(Color.BLACK, 2)); // Borde negro

        areaMensajes = new JTextArea();
        areaMensajes.setEditable(false);
        areaMensajes.setBackground(Color.LIGHT_GRAY);
        panelMensajes.add(new JScrollPane(areaMensajes), BorderLayout.CENTER);

        // Área de texto para el nuevo mensaje
        areaTextoMensaje = new JTextArea(3, 20);
        areaTextoMensaje.setBackground(Color.LIGHT_GRAY);
        
        // Panel para el área de texto y el botón de enviar
        JPanel panelInput = new JPanel();
        panelInput.setLayout(new BorderLayout());
        panelInput.setBackground(Color.DARK_GRAY);
        panelInput.setBorder(new LineBorder(Color.BLACK, 2)); // Borde negro
        panelInput.add(areaTextoMensaje, BorderLayout.CENTER);

        // Botón de enviar
        botonEnviar = new JButton("Enviar");
        botonEnviar.setPreferredSize(new Dimension(80, 30)); // Tamaño del botón
        
        botonEnviar.setActionCommand(ENVIARMENSAJE);
        /*botonEnviar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String mensaje = areaTextoMensaje.getText();
                if (!mensaje.isEmpty()) {
                    String contactoSeleccionado = listaContactos.getSelectedValue();
                    if (contactoSeleccionado != null) {
                        enviarMensaje(mensaje, contactoSeleccionado);
                        areaMensajes.append("Yo: " + mensaje + "\n");
                        areaTextoMensaje.setText("");
                    } else {
                        JOptionPane.showMessageDialog(InterfazMensajeria.this, "Por favor, selecciona un contacto.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });*/
        
        panelInput.add(botonEnviar, BorderLayout.EAST);

        panelMensajes.add(panelInput, BorderLayout.SOUTH);

        // Agregar paneles a la ventana principal
        add(panelContactos, BorderLayout.WEST);
        add(panelMensajes, BorderLayout.CENTER);

        // Estilo de la ventana
        getContentPane().setBackground(Color.DARK_GRAY);
        setVisible(true);

        // Iniciar el servidor en un hilo separado
       // iniciarServidor();
    }

   /* private void iniciarServidor() {
        new Thread(() -> {
            try {
                // Suponiendo que el puerto se obtiene de un contacto o se establece de alguna manera
                int puerto = usuario.getPuerto(); // Cambia esto según sea necesario
                ServerSocket serverSocket = new ServerSocket(puerto);
                areaMensajes.append("Esperando conexiones en el puerto " + puerto + "\n");

                while (true) {
                    Socket soc = serverSocket.accept();
                    areaMensajes.append("Cliente conectado: " + soc.getInetAddress() + "\n");
                    new Thread(new ManejadorCliente(soc)).start();
                }
            } catch (Exception e) {
                areaMensajes.append("Error en el servidor: " + e.getMessage() + "\n");
            }
        }).start();
    }*/
    
    public void formarMensaje() {
        String contenido = areaTextoMensaje.getText();
        if (!contenido.isEmpty()) {
            String contactoSeleccionado = listaContactos.getSelectedValue();
            if (contactoSeleccionado != null) {
                enviarMensaje(contenido, contactoSeleccionado);
                areaMensajes.append("Yo: " + contenido + "\n");
                areaTextoMensaje.setText("");
            } else {
                JOptionPane.showMessageDialog(InterfazMensajeria.this, "Por favor, selecciona un contacto.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    
    public void recibirMensaje(Mensaje mensaje, Socket soc) {
    	if(listaContactos.getSelectedValue() == mensaje.getRemitente()) {
    		// se debe agregar el mensaje al chat
    	}
    	else {
    		Contacto contactoDestino = usuario.buscaContacto(mensaje.getRemitente());
    		// se debe agregar el nuevo contacto a la lista de conversaciones iniciadas y marcarlo como no leido
    	}
    }

    private void enviarMensaje(String contenidoMensaje, String contacto) {
        try {
        	/*
            // Suponiendo que el formato del contacto es "Nombre (IP: 127.0.0.1, Puerto: 12345)"
            String[] partes = contacto.split(" \\(IP: |, Puerto: ");
            String nombre = partes[0];
            String ip = partes[1];
            int puerto = Integer.parseInt(partes[2].replace(")", ""));
			*/
        	
        	Contacto contactoDestino = usuario.buscaContacto(contacto);
        	Mensaje mensaje = new Mensaje(contenidoMensaje, usuario.getNickname());
            Socket socket = new Socket(contactoDestino.getDireccionIP(), contactoDestino.getPuerto());
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(mensaje);
            out.close();
            socket.close();
            usuario.agregarMensaje(mensaje, contacto);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al enviar el mensaje: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void abrirVentanaAgregarContacto() {
        JDialog dialog = new JDialog(this, "Nuevo Contacto", true);
        dialog.setLayout(new GridLayout(3, 2));
        dialog.setSize(300, 200);
        
        JLabel labelNombre = new JLabel("Nombre/Alias:");
        JTextField campoNombre = new JTextField();
        JLabel labelPuerto = new JLabel("Número de Puerto de Comunicación:");
        JTextField campoPuerto = new JTextField();
        JLabel labelID = new JLabel("Dirección IP:");
        JTextField campoIP = new JTextField();
        
        JButton btnAgregar = new JButton("Agregar Contacto");
        btnAgregar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String nombre = campoNombre.getText();
                String puerto = campoPuerto.getText();
                String IP = campoIP.getText();
                if (!nombre.isEmpty() && !puerto.isEmpty() && !IP.isEmpty()) {
                    //modeloContactos.addElement(nombre + " (IP: 127.0.0.1, Puerto: " + puerto + ")");
                	Contacto nuevoContacto = new Contacto(nombre, IP,Integer.parseInt(puerto));
                	Chat nuevoChat = new Chat(nuevoContacto);
                	usuario.agregarChat(nuevoChat);
                	usuario.agregarContacto(nuevoContacto);
                    modeloContactos.addElement(nombre);
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Por favor, complete todos los campos.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        dialog.add(labelNombre);
        dialog.add(campoNombre);
        dialog.add(labelPuerto);
        dialog.add(campoPuerto);
        dialog.add(labelID);
        dialog.add(campoIP);
        dialog.add(btnAgregar);
        
        dialog.setVisible(true);
    }
    
    public JTextArea getAreaMensajes() {
		return areaMensajes;
	}

	public void setAreaMensajes(JTextArea areaMensajes) {
		this.areaMensajes = areaMensajes;
	}
    
    public void setControlador(Controlador c) {
    	botonEnviar.addActionListener(c);
    	btnAgregarContacto.addActionListener(c);
    }
}