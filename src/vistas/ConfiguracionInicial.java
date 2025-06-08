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

    // Opciones de almacenamiento
    private static final String[] OPCIONES_FORMATO = {"XML", "JSON", "Texto plano"};

    public ConfiguracionInicial() {
        setTitle("Configuración Inicial");
        setSize(500, 300);
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

        String[] formatos = {"xml", "json", "txt"};
        String formatoSeleccionado = null;
        File archivoExistente = null;
        int tipoAlmacenamiento = 0;

        for (int i = 0; i < formatos.length; i++) {
            File f = new File(nickname + "." + formatos[i]);
            if (f.exists()) {
                archivoExistente = f;
                formatoSeleccionado = formatos[i];
                tipoAlmacenamiento = i + 1;
                break;
            }
        }

        if (archivoExistente == null) {
            int opcion = showOptionDialogPersonalizado(
                "Selecciona el formato de almacenamiento local para tus mensajes:",
                "Tipo de archivo para mensajes",
                OPCIONES_FORMATO, 0
            );
            if (opcion == -1) {
                return;
            }
            formatoSeleccionado = formatos[opcion];
            tipoAlmacenamiento = opcion + 1;
            File nuevoArchivo = new File(nickname + "." + formatoSeleccionado);
            try {
                if (nuevoArchivo.createNewFile()) {
                    if (formatoSeleccionado.equals("xml")) {
                        try (FileWriter fw = new FileWriter(nuevoArchivo)) {
                            fw.write("<mensajes></mensajes>");
                        }
                    } else if (formatoSeleccionado.equals("json")) {
                        try (FileWriter fw = new FileWriter(nuevoArchivo)) {
                            fw.write("[]");
                        }
                    }
                }
            } catch (IOException ex) {
                showErrorDialog("No se pudo crear el archivo de mensajes: " + ex.getMessage());
                return;
            }
        }

        InetAddress direccion = null;
        try {
            direccion = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        String ip = direccion.getHostAddress();
        Usuario usuario = new Usuario.Builder()
        	    .setNickname(nickname)
        	    .setPuerto(puerto)
        	    .setIp(ip)
        	    .setTipoAlmacenamiento(tipoAlmacenamiento)
        	    .build();
        dispose();

        Socket socket = null;
        ObjectOutputStream out = null;

        // Nuevo: Ventana personalizada para elegir algoritmo de cifrado
        String[] algoritmos = {"AES", "DES", "Triple DES"};
        int seleccion = showOptionDialogPersonalizado(
            "Seleccione el tipo de cifrado a usar:", "Cifrado", algoritmos, 0
        );
        if (seleccion == -1) return;
        String algoritmoElegido = algoritmos[seleccion];

        // Nuevo: Ventana personalizada para ingresar la clave
        String clave = showTextInputPersonalizado(
            "Ingrese la clave de cifrado:",
            "Clave de cifrado"
        );
        if (clave == null || clave.isEmpty()) {
            showErrorDialog("Debe ingresar una clave.");
            return;
        }

        try {
            InterfazMensajeria vista = new InterfazMensajeria(usuario, algoritmoElegido, clave);
            Controlador controlador = Controlador.getInstance(vista, usuario);
            vista.setControlador(controlador);
            vista.setVisible(true);

            socket = new Socket("localhost", 10001);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(usuario);
        } catch (IOException e) {
            showErrorDialog("No se pudo conectar al servidor: " + e.getMessage());
        } finally {
            try {
                if (out != null) out.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Diálogo personalizado de selección de opciones tipo radio
    private int showOptionDialogPersonalizado(String msg, String title, String[] options, int defaultOption) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setUndecorated(false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(420, 180); // Ajustable

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COLOR_BG);

        JLabel mensaje = new JLabel(msg);
        mensaje.setFont(new Font(FUENTE, Font.PLAIN, 15));
        mensaje.setForeground(COLOR_LABEL);
        mensaje.setBorder(BorderFactory.createEmptyBorder(18, 24, 0, 24));
        panel.add(mensaje, BorderLayout.NORTH);

        JPanel opcionesPanel = new JPanel();
        opcionesPanel.setBackground(COLOR_BG);
        ButtonGroup grupo = new ButtonGroup();
        JRadioButton[] radios = new JRadioButton[options.length];
        for (int i = 0; i < options.length; i++) {
            radios[i] = new JRadioButton(options[i]);
            radios[i].setForeground(COLOR_LABEL);
            radios[i].setBackground(COLOR_BG);
            radios[i].setFont(new Font(FUENTE, Font.PLAIN, 14));
            grupo.add(radios[i]);
            opcionesPanel.add(radios[i]);
            if (i == defaultOption) radios[i].setSelected(true);
        }
        panel.add(opcionesPanel, BorderLayout.CENTER);

        JButton ok = new JButton("Aceptar");
        ok.setBackground(COLOR_BTN);
        ok.setForeground(Color.WHITE);
        ok.setFont(new Font(FUENTE, Font.BOLD, 14));
        ok.setFocusPainted(false);

        final int[] respuesta = {-1};
        ok.addActionListener(ev -> {
            for (int i = 0; i < radios.length; i++)
                if (radios[i].isSelected()) respuesta[0] = i;
            dialog.dispose();
        });
        JPanel panelOk = new JPanel();
        panelOk.setBackground(COLOR_BG);
        panelOk.add(ok);
        panel.add(panelOk, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        return respuesta[0];
    }

    // Diálogo personalizado de input de texto
    private String showTextInputPersonalizado(String mensaje, String title) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setUndecorated(false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(420, 145);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COLOR_BG);

        JLabel label = new JLabel(mensaje);
        label.setFont(new Font(FUENTE, Font.PLAIN, 15));
        label.setForeground(COLOR_LABEL);
        label.setBorder(BorderFactory.createEmptyBorder(18, 24, 8, 24));
        panel.add(label, BorderLayout.NORTH);

        JTextField campo = new JTextField();
        campo.setBackground(COLOR_CUADRO);
        campo.setForeground(COLOR_LABEL);
        campo.setFont(new Font(FUENTE, Font.PLAIN, 15));
        campo.setBorder(BorderFactory.createLineBorder(COLOR_LABEL.darker(), 1));
        campo.setCaretColor(COLOR_LABEL);
        campo.setMargin(new Insets(6,8,6,8));
        panel.add(campo, BorderLayout.CENTER);

        JButton ok = new JButton("Aceptar");
        ok.setBackground(COLOR_BTN);
        ok.setForeground(Color.WHITE);
        ok.setFont(new Font(FUENTE, Font.BOLD, 14));
        ok.setFocusPainted(false);

        final String[] res = {null};
        ok.addActionListener(ev -> {
            res[0] = campo.getText();
            dialog.dispose();
        });
        campo.addActionListener(ev -> {
            res[0] = campo.getText();
            dialog.dispose();
        });

        JPanel panelOk = new JPanel();
        panelOk.setBackground(COLOR_BG);
        panelOk.add(ok);

        panel.add(panelOk, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        return res[0];
    }

    private boolean puertoDisponible(int puerto) {
        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void showErrorDialog(String msg) {
        JDialog dialog = new JDialog(this, "Error", true);
        dialog.setUndecorated(false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(COLOR_BG);

        JTextArea label = new JTextArea(msg);
        label.setEditable(false);
        label.setForeground(COLOR_LABEL);
        label.setBackground(COLOR_BG);
        label.setFont(new Font(FUENTE, Font.PLAIN, 15));
        label.setWrapStyleWord(true);
        label.setLineWrap(true);
        label.setBorder(BorderFactory.createEmptyBorder(18, 24, 10, 24));
        label.setFocusable(false);

        // Si el mensaje puede ser muy largo, emplea JScrollPane.
        JScrollPane scroll = new JScrollPane(label);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setBackground(COLOR_BG);
        scroll.getViewport().setBackground(COLOR_BG);

        contentPanel.add(scroll, BorderLayout.CENTER);

        JButton cerrar = new JButton("Cerrar");
        cerrar.setBackground(new Color(80, 140, 200));
        cerrar.setForeground(Color.WHITE);
        cerrar.setFont(new Font(FUENTE, Font.BOLD, 13));
        cerrar.setFocusPainted(false);
        cerrar.setBorder(BorderFactory.createEmptyBorder(7, 18, 7, 18));
        cerrar.addActionListener(e -> dialog.dispose());

        JPanel panelBoton = new JPanel();
        panelBoton.setBackground(COLOR_BG);
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
}