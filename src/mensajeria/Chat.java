package mensajeria;
import java.util.*;
public class Chat {
	private Contacto contacto;
    private LinkedList<Mensaje> mensajes;

    public Chat(Contacto contacto) {
        this.contacto = contacto;
        this.mensajes = new LinkedList<>();
    }

    public void agregarMensaje(Mensaje mensaje) {
        mensajes.addLast(mensaje);;
    }
    
    
    public LinkedList<Mensaje> getMensajes() { return mensajes; }
    public Contacto getContacto() { return contacto; }
}
