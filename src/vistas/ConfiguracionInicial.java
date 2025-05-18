package vistas;

import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import controlador.*;
import mensajeria.Usuario;

@SuppressWarnings("serial")
public class ConfiguracionInicial extends JFrame {
    private JTextField nicknameField;
    private JTextField puertoField;

    private static final String FUENTE = "Segoe UI";
    private static final Color COLOR_BG = new Color(34, 44, 54);
    private static final Color COLOR_LABEL = new Color(222, 222, 240);
    private static final Color COLOR_CUADRO = new Color(28, 38, 48);
    private static final Color COLOR_BTN = new Color(80, 140, 200);

    public ConfiguracionInicial() {
        setTitle("Configuración Inicial");
        setSize(380, 220);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(COLOR_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(14, 18, 6, 18);

        JLabel nickLabel = new JLabel("Nickname:");
        nickLabel.setForeground(COLOR_LABEL);
        nickLabel.setFont(new Font(FUENTE, Font.PLAIN, 16));
        gbc.gridx = 0; gbc.gridy = 0;
        mainPanel.add(nickLabel, gbc);

        nicknameField = new JTextField(16);
        nicknameField.setBackground(COLOR_CUADRO);
        nicknameField.setForeground(COLOR_LABEL);
        nicknameField.setFont(new Font(FUENTE, Font.PLAIN, 15));
        nicknameField.setCaretColor(COLOR_LABEL);
        nicknameField.setBorder(BorderFactory.createLineBorder(COLOR_LABEL.darker(), 1));
        gbc.gridx = 1; gbc.weightx = 1.0;
        mainPanel.add(nicknameField, gbc);

        JLabel puertoLabel = new JLabel("Puerto:");
        puertoLabel.setForeground(COLOR_LABEL);
        puertoLabel.setFont(new Font(FUENTE, Font.PLAIN, 16));
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.weightx = 0;
        mainPanel.add(puertoLabel, gbc);

        puertoField = new JTextField(16);
        puertoField.setBackground(COLOR_CUADRO);
        puertoField.setForeground(COLOR_LABEL);
        puertoField.setFont(new Font(FUENTE, Font.PLAIN, 15));
        puertoField.setCaretColor(COLOR_LABEL);
        puertoField.setBorder(BorderFactory.createLineBorder(COLOR_LABEL.darker(), 1));
        gbc.gridx = 1; gbc.weightx = 1.0;
        mainPanel.add(puertoField, gbc);

        JButton iniciarBtn = new JButton("Iniciar");
        iniciarBtn.setFont(new Font(FUENTE, Font.BOLD, 15));
        iniciarBtn.setBackground(COLOR_BTN);
        iniciarBtn.setForeground(Color.WHITE);
        iniciarBtn.setFocusPainted(false);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.insets = new Insets(22, 60, 14, 60);
        gbc.weightx = 0;
        mainPanel.add(iniciarBtn, gbc);

        iniciarBtn.addActionListener(e -> iniciarAplicacion());
        nicknameField.addActionListener(e -> iniciarAplicacion());
        puertoField.addActionListener(e -> iniciarAplicacion());

        setContentPane(mainPanel);
        getContentPane().setBackground(COLOR_BG);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void iniciarAplicacion() {
        String nickname = nicknameField.getText().trim();
        String puertoTexto = puertoField.getText().trim();

        if (nickname.isEmpty() || puertoTexto.isEmpty()) {
            showErrorDialog("Todos los campos son obligatorios.");
            return;
        }

        int puerto;
        try {
            puerto = Integer.parseInt(puertoTexto);
            if (puerto < 1024 || puerto > 65535 || !puertoDisponible(puerto)) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            showErrorDialog("Ingrese un puerto válido (1024-65535 y disponible).");
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
            InterfazMensajeria vista = new InterfazMensajeria(usuario);
            Controlador controlador = new Controlador(vista, usuario);
            vista.setControlador(controlador);
            vista.setVisible(true);

            socket = new Socket("localhost", 10001);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(usuario);

        } catch (IOException e) {
            showErrorDialog("No se pudo conectar al servidor: " + e.getMessage());
        } finally {
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

    private void showErrorDialog(String msg) {
        JTextArea label = new JTextArea(msg);
        label.setEditable(false);
        label.setForeground(new Color(255, 255, 255));
        label.setBackground(COLOR_BG);
        label.setFont(new Font(FUENTE, Font.PLAIN, 15));
        label.setWrapStyleWord(true);
        label.setLineWrap(true);
        label.setMargin(new Insets(12, 12, 12, 12));
        JOptionPane.showMessageDialog(
            this, label, "Error", JOptionPane.ERROR_MESSAGE
        );
    }
}