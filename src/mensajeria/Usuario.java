package mensajeria;
import java.io.*;
import java.net.*;
import java.util.*;


public class Usuario {
	 private String nickname;
	    private int puerto;
	    private HashMap<String, Contacto> agenda;
	    private HashMap<String, Chat> conversaciones;

		public Usuario(String nickname, int puerto) {
	        this.nickname = nickname;
	        this.puerto = puerto;
	        this.agenda = new HashMap<>();
	        this.conversaciones = new HashMap<>();
	    }

	    public void agregarContacto(Contacto contacto) {
	    	agenda.put(contacto.getNombre(),contacto);
	    }
	    
	    public void agregarChat(Chat chat) {
	    	conversaciones.put(chat.getContacto().getNombre(), chat);
	    }
	    
	    public void agregarMensaje(Mensaje mensaje, String contactoDestino) {
	    	conversaciones.get(contactoDestino).agregarMensaje(mensaje);
	    }

	    /*public void iniciarConversacion(String nombreContacto) {
	        for (Contacto c : contactos) {
	            if (c.getNombre().equals(nombreContacto)) {
	                conversaciones.put(nombreContacto, new Chat(c));
	                break;
	            }
	        }
	    }

	    public void enviarMensaje(String nombreContacto, String contenido) {
	        Chat chat = conversaciones.get(nombreContacto);
	        if (chat != null) {
	            Mensaje mensaje = new Mensaje(contenido, nickname);
	            chat.agregarMensaje(mensaje);
	            enviarMensajeRed(chat.getContacto(), contenido);
	        }
	    }

	    private void enviarMensajeRed(Contacto contacto, String contenido) {
	        try (Socket socket = new Socket(contacto.getDireccionIP(), contacto.getPuerto());
	             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
	            out.println(nickname + ": " + contenido);
	        } catch (IOException e) {
	            System.out.println("Error al enviar mensaje a " + contacto.getNombre());
	        }
	    }*/

	    public HashMap<String, Chat> getConversaciones() {
			return conversaciones;
		}
	    
	    public HashMap<String, Contacto> getAgenda() {
			return agenda;
		}
	    
		public String getNickname() {
			return nickname;
		}

		public void setNickname(String nickname) {
			this.nickname = nickname;
		}

		public int getPuerto() {
			return puerto;
		}

		public void setPuerto(int puerto) {
			this.puerto = puerto;
		}

		@Override
		public String toString() {
			return  nickname + ", puerto = " + puerto;
		}

		public Contacto buscaContacto(String contacto) {
			return this.agenda.get(contacto);
		}
	    
		public Chat buscaChat(String nombreContacto) {
		    return conversaciones.get(nombreContacto);
		}
		
	
}
