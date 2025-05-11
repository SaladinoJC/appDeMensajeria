package vistas;
import java.io.*;
import java.net.*;
import javax.swing.*;

import controlador.*;
import mensajeria.Usuario;

import java.awt.*;

@SuppressWarnings("serial")
public class ConfiguracionInicial extends JFrame {
    private JTextField nicknameField;
    private JTextField puertoField;

    public ConfiguracionInicial() {
        setTitle("Configuración Inicial");
        setSize(300, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(3, 2));

        add(new JLabel("Nickname:"));
        nicknameField = new JTextField();
        add(nicknameField);

        add(new JLabel("Puerto:"));
        puertoField = new JTextField();
        add(puertoField);

        JButton iniciarBtn = new JButton("Iniciar");
        iniciarBtn.addActionListener(e -> iniciarAplicacion());
        add(iniciarBtn);
        
        nicknameField.addActionListener(e -> iniciarAplicacion());
        puertoField.addActionListener(e -> iniciarAplicacion());

        setVisible(true);
    }

    private void iniciarAplicacion() {
        String nickname = nicknameField.getText().trim();
        String puertoTexto = puertoField.getText().trim();
        
        if (nickname.isEmpty() || puertoTexto.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Todos los campos son obligatorios.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int puerto;
        try {
            puerto = Integer.parseInt(puertoTexto);
            if (puerto < 1024 || puerto > 65535 || !puertoDisponible(puerto)) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Ingrese un puerto válido (1024-65535 y disponible).", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        InetAddress direccion = null;
        try {
            direccion = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        String ip = direccion.getHostAddress();
        Usuario usuario = new Usuario(nickname, puerto, ip);
        dispose();

        Socket socket = null;
        ObjectOutputStream out = null;

        try {
            // Crear la vista y el controlador con los objetos correspondientes
            InterfazMensajeria vista = new InterfazMensajeria(usuario);
            Controlador controlador = new Controlador(vista, usuario);
            vista.setControlador(controlador);
            vista.setVisible(true);

            // Conectar al servidor
            socket = new Socket("localhost", 10001); // o la IP del servidor
            out = new ObjectOutputStream(socket.getOutputStream());

            // Enviar mensaje inicial de conexión al servidor
            out.writeObject(usuario);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "No se pudo conectar al servidor: " + e.getMessage(),
                    "Error de conexión", JOptionPane.ERROR_MESSAGE);
        } finally {
            // Asegurarse de cerrar los recursos (Socket y ObjectOutputStream)
            try {
                if (out != null) {
                    out.close();
                }
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private boolean puertoDisponible(int puerto) {
        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
