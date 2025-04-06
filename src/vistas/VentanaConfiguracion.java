package vistas;

import javax.swing.*;

import mensajeria.Usuario;

import java.awt.*;
import java.awt.event.*;

public class VentanaConfiguracion extends JDialog {

    private JTextField campoNombre;
    private JTextField campoPuerto;
    private Usuario usuario;
    private InterfazMensajeria interfazmensajeria;

    public VentanaConfiguracion(InterfazMensajeria interfazMensajeria, Usuario usuario) {
    	super(interfazMensajeria, "Configuración", true);
    	this.interfazmensajeria = interfazMensajeria;
        this.usuario = usuario;

        setLayout(new GridLayout(3, 2, 10, 10));
        setSize(300, 150);
        setLocationRelativeTo(interfazmensajeria);

        // Campo para el nombre
        add(new JLabel("Nombre de usuario:"));
        campoNombre = new JTextField(usuario.getNickname());
        add(campoNombre);

        // Campo para el puerto
        add(new JLabel("Puerto:"));
        campoPuerto = new JTextField(String.valueOf(usuario.getPuerto()));
        add(campoPuerto);

        // Botón de guardar
        JButton botonGuardar = new JButton("Guardar");
        botonGuardar.addActionListener(e -> guardarConfiguracion());
        add(new JLabel()); // espacio vacío
        add(botonGuardar);
    }

    private void guardarConfiguracion() {
        String nuevoNombre = campoNombre.getText().trim();
        String textoPuerto = campoPuerto.getText().trim();

        if (nuevoNombre.isEmpty() || !textoPuerto.matches("\\d+")) {
            JOptionPane.showMessageDialog(this, "Verifica los campos ingresados.");
            return;
        }

        int nuevoPuerto = Integer.parseInt(textoPuerto);
        usuario.setNickname(nuevoNombre);
        usuario.setPuerto(nuevoPuerto);
        interfazmensajeria.setTitle(nuevoNombre + " - Puerto " + nuevoPuerto);

        JOptionPane.showMessageDialog(this, "Datos actualizados correctamente.");
        dispose();
    }
}
