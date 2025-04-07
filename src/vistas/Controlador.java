package vistas;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

import mensajeria.Mensaje;
import mensajeria.Usuario;

public class Controlador implements ActionListener {
	private InterfazMensajeria vistaPrincipal;
	private Usuario usuario;
	private ServerSocket serverSocket;
	private Thread hiloServidor;
	private boolean escuchando = true;
	
	
	public Controlador(InterfazMensajeria vistaPrincipal, Usuario usuario) {
		this.usuario = usuario;
		this.vistaPrincipal = vistaPrincipal;
		// Iniciar el servidor en un hilo separado
		iniciarServidor();
	}
	
	private void iniciarServidor() {
	    escuchando = true;
	    hiloServidor = new Thread(() -> {
	        try {
	            serverSocket = new ServerSocket(usuario.getPuerto());
	            while (escuchando) {
	                Socket soc = serverSocket.accept();
	                ObjectInputStream input = new ObjectInputStream(soc.getInputStream());
	                Mensaje mensaje = (Mensaje) input.readObject();
	                this.vistaPrincipal.recibirMensaje(mensaje, soc);
	            }
	        } catch (Exception e) {
	            if (escuchando) e.printStackTrace(); // solo si no fue cerrado intencionalmente
	        }
	    });
	    hiloServidor.start();
	}
	
	
public void actualizarPuerto(int nuevoPuerto) {
    try {
        escuchando = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close(); // Cerramos el socket anterior
        }
        usuario.setPuerto(nuevoPuerto);
        iniciarServidor(); // Lo volvemos a iniciar con el nuevo puerto
    } catch (Exception e) {
        e.printStackTrace();
    }
}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equalsIgnoreCase(InterfazVista.ABRIRVENTAGREGARCONTACTO)) {
			this.vistaPrincipal.abrirVentanaAgregarContacto();
		}
		else if(e.getActionCommand().equalsIgnoreCase(InterfazVista.ENVIARMENSAJE)) {
			this.vistaPrincipal.formarMensaje();
		}
	}

}
