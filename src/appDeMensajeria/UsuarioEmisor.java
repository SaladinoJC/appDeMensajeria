package appDeMensajeria;
import java.io.*;
import java.net.*;
import java.util.*;


public class UsuarioEmisor {
	 private String nickname;
	    private int puerto;
	    private List<Contacto> contactos;
	    private Map<String, Chat> conversaciones;

	    public UsuarioEmisor(String nickname, int puerto) {
	        this.nickname = nickname;
	        this.puerto = puerto;
	        this.contactos = new ArrayList<>();
	        this.conversaciones = new HashMap<>();
	    }

	    public void agregarContacto(Contacto contacto) {
	        contactos.add(contacto);
	    }

	    public void iniciarConversacion(String nombreContacto) {
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
	    
}
