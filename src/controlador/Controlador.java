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

    private static Controlador instanciaUnica = null;
    private InterfazMensajeria vistaPrincipal;
    private Usuario usuario;
    private Thread hiloReceptorMensajes;
    private boolean escuchando = true;

    // Constructor PRIVADO
    private Controlador(InterfazMensajeria vistaPrincipal, Usuario usuario) {
        this.usuario = usuario;
        this.vistaPrincipal = vistaPrincipal;
        this.iniciarRecibeMensajes();
    }

    // PATRÓN SINGLETON: único punto de acceso
    public static synchronized Controlador getInstance(InterfazMensajeria vistaPrincipal, Usuario usuario) {
        if (instanciaUnica == null) {
            instanciaUnica = new Controlador(vistaPrincipal, usuario);
        } else {
            // Solo si necesitas actualizar la vista y usuario en reuso:
            instanciaUnica.vistaPrincipal = vistaPrincipal;
            instanciaUnica.usuario = usuario;
        }
        return instanciaUnica;
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
                if (escuchando) e.printStackTrace();
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