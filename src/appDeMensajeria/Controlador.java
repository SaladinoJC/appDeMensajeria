package appDeMensajeria;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.ServerSocket;
import java.net.Socket;

public class Controlador implements ActionListener {
	private InterfazMensajeria vistaPrincipal;
	private UsuarioEmisor usuario;
	
	
	public Controlador(InterfazMensajeria vistaPrincipal, UsuarioEmisor usuario) {
		this.usuario = usuario;
		this.vistaPrincipal = vistaPrincipal;
		//iniciarServidor();
	}
	
	/*private void iniciarServidor() {
	        new Thread(() -> {
	            try {
	                // Suponiendo que el puerto se obtiene de un contacto o se establece de alguna manera
	                int puerto = usuario.getPuerto(); // Cambia esto según sea necesario
	                ServerSocket serverSocket = new ServerSocket(puerto);
	                this.vistaPrincipal.getAreaMensajes().append("Esperando conexiones en el puerto " + puerto + "\n");

	                while (true) {
	                    Socket soc = serverSocket.accept();
	                    this.vistaPrincipal.getAreaMensajes().append("Cliente conectado: " + soc.getInetAddress() + "\n");
	                    new Thread(new ManejadorCliente(soc)).start();
	                }
	            } catch (Exception e) {
	                this.vistaPrincipal.getAreaMensajes().append("Error en el servidor: " + e.getMessage() + "\n");
	            }
	        }).start();
	  }*/

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
