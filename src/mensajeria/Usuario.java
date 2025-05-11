package mensajeria;

import java.io.*;
import java.util.*;

@SuppressWarnings("serial")
public class Usuario implements Serializable {

    private String nickname;
    private int puerto;
    private String direccionIP;
    private HashMap<String, Contacto> agenda;
    private HashMap<String, Chat> conversaciones;

    public Usuario(String nickname, int puerto, String direccionIP) {
        this.nickname = nickname;
        this.direccionIP = direccionIP;
        this.puerto = puerto;
        this.agenda = new HashMap<>();
        this.conversaciones = new HashMap<>();
    }

    public void agregarContacto(Contacto contacto) {
        agenda.put(contacto.getNombre(), contacto);
    }

    public void agregarChat(Chat chat) {
        conversaciones.put(chat.getContacto().getNombre(), chat);
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

}