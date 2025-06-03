package mensajeria;
import java.io.Serializable;
import java.util.*;
import java.util.List;
public class Chat implements Serializable{
	private Contacto contacto;
    private List<Mensaje> mensajes;
    public Chat(Contacto contacto) {
        this.contacto = contacto;
        this.mensajes = new ArrayList<>();
    }
    public void agregarMensaje(Mensaje mensaje) {
        mensajes.addLast(mensaje);;
    }
    
    public void setMensajes(List<Mensaje> mensajes) {
        this.mensajes = mensajes != null ? mensajes : new ArrayList<>();
    }
    public List<Mensaje> getMensajes() { return mensajes; }
    public Contacto getContacto() { return contacto; }
}
