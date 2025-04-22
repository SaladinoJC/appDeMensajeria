package mensajeria;
import java.io.*;
import java.net.*;
import java.util.*;


public class Usuario implements Serializable{
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
