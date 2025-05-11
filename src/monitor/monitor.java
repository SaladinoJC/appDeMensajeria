package monitor;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import javax.swing.SwingUtilities;

import mensajeria.Mensaje;
import servidor.Servidor;

public class monitor {
	public LinkedList<Servidor> servidoresDisponibles = new LinkedList<>();
	public int puertoEscuchaServidor = 10009;
	
	public monitor(){
		iniciarMonitor();
	}
	
	private void iniciarMonitor() {
		 	boolean escuchando = true;
	        Thread hiloReceptorEstadoServidor = new Thread(() -> {
	        	
	            try (ServerSocket serverSocketMensajes = new ServerSocket(this.puertoEscuchaServidor)) {
	                while (escuchando) {
	                    Socket socketRecibeMensaje = serverSocketMensajes.accept();
	                    try (ObjectInputStream input = new ObjectInputStream(socketRecibeMensaje.getInputStream())) {
	                    	Servidor server = (Servidor) input.readObject();
	                    	if(servidoresDisponibles.isEmpty()) {
	                    		this.servidoresDisponibles.add(server);
	                    		activarServidorPrimario(server);
	                    		pingEcho();
	                    	}
	                    	else
	                    		this.servidoresDisponibles.add(server);
	                        
	                    } catch (Exception e) {
	                        e.printStackTrace();
	                    }
	                }
	            }
	            catch (Exception e) {
	                if (escuchando) e.printStackTrace(); // Solo si no fue cerrado intencionalmente
	            }
	        });
	        hiloReceptorEstadoServidor.start();
	}
	
	
	private void activarServidorPrimario(Servidor servidor) {
		try {
			Socket socketAux = new Socket("localhost", servidor.getPuertoEstado());
			ObjectOutputStream output = new  ObjectOutputStream(socketAux.getOutputStream());
			output.writeBytes("primario");
			socketAux.close();
		} 
		
		catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
		catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	private void pingEcho() {
		 Thread hiloPreguntaEstadoServidor = new Thread(()-> {
			 while(true) {
				 Iterator iterator = this.servidoresDisponibles.iterator();
				 try{
					 Servidor servidorActual = (Servidor) iterator.next();
					 Socket socketPrincipal = new Socket("localhost", servidorActual.getPuertoEstado());
					 socketPrincipal.connect(socketPrincipal.getLocalSocketAddress(), 2000);
					 Socket socketAux = new Socket("localhost", servidorActual.getPuertoEstado());
					 
					 ObjectOutputStream output = new  ObjectOutputStream(socketAux.getOutputStream());
					 output.writeBytes("secundario");
				 }
		         
		         catch (Exception e) {
		        	iterator.remove();
		        	
		        	Servidor servidor;
					if(iterator.hasNext()) {
						servidor = (Servidor) iterator.next();
					}
		        	 else {
		        		 iterator = this.servidoresDisponibles.iterator();
		        		 servidor = (Servidor) iterator.next();
		        	 }
					
					activarServidorPrimario(servidor);
		 		} 
			}
		 });
	}

	
	public static void main(String[] args) {
		 SwingUtilities.invokeLater(() -> new monitor());
	}

}
