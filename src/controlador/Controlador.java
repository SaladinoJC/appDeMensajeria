package controlador;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import javax.swing.JOptionPane;
import mensajeria.Mensaje;
import mensajeria.Usuario;
import vistas.InterfazMensajeria;
import vistas.InterfazVista;

public class Controlador implements ActionListener {

    private InterfazMensajeria vistaPrincipal;
    private Usuario usuario;
    private Thread hiloReceptorMensajes;
    private boolean escuchando = true;

    public Controlador(InterfazMensajeria vistaPrincipal, Usuario usuario) {
        this.usuario = usuario;
        this.vistaPrincipal = vistaPrincipal;
        // Iniciar el servidor en un hilo separado
        this.iniciarRecibeMensajes();
    }

    // Método para iniciar el hilo que escucha mensajes
    private void iniciarRecibeMensajes() {
        escuchando = true;
        hiloReceptorMensajes = new Thread(() -> {
            try (ServerSocket serverSocketMensajes = new ServerSocket(usuario.getPuerto())) {
                while (escuchando) {
                    Socket socketRecibeMensaje = serverSocketMensajes.accept();
                    try (ObjectInputStream input = new ObjectInputStream(socketRecibeMensaje.getInputStream())) {
                        Mensaje mensaje = (Mensaje) input.readObject();
                        this.vistaPrincipal.recibirMensaje(mensaje, socketRecibeMensaje, this.usuario);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                if (escuchando) e.printStackTrace(); // Solo si no fue cerrado intencionalmente
            }
        });
        hiloReceptorMensajes.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equalsIgnoreCase(InterfazVista.ABRIRVENTAGREGARCONTACTO)) {
            abrirVentanaAgregarContacto();
        } else if (e.getActionCommand().equalsIgnoreCase(InterfazVista.ENVIARMENSAJE)) {
            this.vistaPrincipal.formarMensaje(this.usuario);
        }
    }

    private void abrirVentanaAgregarContacto() {
        try (
        	  Socket socket = new Socket("localhost", 10002);
        	  ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
            ) {

            @SuppressWarnings("unchecked")
            HashMap<String, Usuario> directorioUsuarios = (HashMap<String, Usuario>) in.readObject();
            this.vistaPrincipal.abrirVentanaAgregarContacto(directorioUsuarios, this.usuario);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                this.vistaPrincipal,
                "Error al pedir la lista de usuarios registrados en el servidor.",
                "Lista de usuarios registrados en el servidor.",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

}
