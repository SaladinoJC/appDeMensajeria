package mensajeria;

import java.io.*;
import java.util.*;

@SuppressWarnings("serial")
public class Usuario implements Serializable, Observer {
	private String nickname;
	private int puerto;
	private String direccionIP;
	private HashMap<String, Contacto> agenda;
	private HashMap<String, Chat> conversaciones;
	private int tipoAlmacenamiento; // 1:XML, 2:JSON, 3:TXT

	private Usuario(Builder builder) {
	    this.nickname = builder.nickname;
	    this.direccionIP = builder.direccionIP;
	    this.puerto = builder.puerto;
	    this.tipoAlmacenamiento = builder.tipoAlmacenamiento;
	    this.agenda = new HashMap<>();
	    this.conversaciones = new HashMap<>();
	}

	public void agregarContacto(Contacto contacto) {
	    agenda.put(contacto.getNombre(), contacto);
	}

	public void agregarChat(Chat chat) {
	    conversaciones.put(chat.getContacto().getNombre(), chat);
	}
	
	
	@Override
    public void actualizar(String tipo, Object dato) {
        if( tipo.equalsIgnoreCase("NUEVO_CONTACTO") ) {
                Contacto contacto = (Contacto) dato;
                agregarContacto(contacto);
        }
    }

	public void agregarMensaje(Mensaje mensaje, String contactoDestino) {
	    Chat chat = conversaciones.get(contactoDestino);
	    if (chat == null) {
	        Contacto contacto = agenda.get(contactoDestino);
	        if (contacto == null) {
	            // Si no está en la agenda, no se puede crear el chat, debería manejarse de otro modo
	            return;
	        }
	        chat = new Chat(contacto);
	        conversaciones.put(contactoDestino, chat);
	    }
	    chat.agregarMensaje(mensaje);
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

	public int getTipoAlmacenamiento() { return tipoAlmacenamiento; }
	public void setTipoAlmacenamiento(int tipo) { this.tipoAlmacenamiento = tipo; }

	public String getIp() {
	    return direccionIP;
	}

	@Override
	public String toString() {
	    return nickname + ", puerto = " + puerto;
	}

	public Contacto buscaContactoPorNombre(String nombre) {
	    for (Contacto contacto : agenda.values()) {
	        if (contacto.getNombre().equals(nombre)) {
	            return contacto;
	        }
	    }
	    return null; // Si no lo encuentra
	}

	public Contacto buscaContactoPorNickname(String nickname) {
	    for (Contacto contacto : agenda.values()) {
	        if (contacto.getNickname().equals(nickname)) {
	            return contacto;
	        }
	    }
	    return null; // Si no lo encuentra
	}


	public Chat buscaChat(String nombreContacto) {
	    return conversaciones.get(nombreContacto);
	}
	
	public static class Builder {
	    private String nickname;
	    private int puerto;
	    private String direccionIP;
	    private int tipoAlmacenamiento;

	    public Builder setNickname(String nickname) {
	        this.nickname = nickname;
	        return this;
	    }
	    public Builder setPuerto(int puerto) {
	        this.puerto = puerto;
	        return this;
	    }
	    public Builder setIp(String direccionIP) {
	        this.direccionIP = direccionIP;
	        return this;
	    }
	    public Builder setTipoAlmacenamiento(int tipoAlmacenamiento) {
	        this.tipoAlmacenamiento = tipoAlmacenamiento;
	        return this;
	    }
	    public Usuario build() {
	        return new Usuario(this);
	    }
	}
	
	
	
}