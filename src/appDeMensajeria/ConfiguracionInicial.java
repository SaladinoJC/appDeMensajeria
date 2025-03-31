package appDeMensajeria;
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;

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

        UsuarioEmisor usuario = new UsuarioEmisor(nickname, puerto);
        dispose();
        SwingUtilities.invokeLater(() -> new InterfazMensajeria(usuario));
    }

    private boolean puertoDisponible(int puerto) {
        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
