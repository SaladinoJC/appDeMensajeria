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
	
	
	public Controlador(InterfazMensajeria vistaPrincipal, Usuario usuario) {
		this.usuario = usuario;
		this.vistaPrincipal = vistaPrincipal;
		// Iniciar el servidor en un hilo separado
		iniciarServidor();
	}
	
	private void iniciarServidor() {
	        new Thread(() -> {
	            try {
	                // Suponiendo que el puerto se obtiene de un contacto o se establece de alguna manera
	                int puerto = usuario.getPuerto(); // Cambia esto según sea necesario
	                ServerSocket serverSocket = new ServerSocket(puerto);
	                //this.vistaPrincipal.getAreaMensajes().append("Esperando conexiones en el puerto " + puerto + "\n");

	                while (true) {
	                    Socket soc = serverSocket.accept();
	                    //this.vistaPrincipal.getAreaMensajes().append("Mensaje recibido de IP:" + soc.getInetAddress() + "\n");
	                    ObjectInputStream input = new ObjectInputStream(soc.getInputStream());
	                    Mensaje mensaje = (Mensaje) input.readObject();
	                    this.vistaPrincipal.recibirMensaje(mensaje, soc);
	                    //new Thread(new ManejadorCliente(soc)).start();
	                }
	            } catch (Exception e) {

	            }
	        }).start();
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
