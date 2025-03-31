package appDeMensajeria;
import java.util.*;
public class Chat {
	private Contacto contacto;
    private List<Mensaje> mensajes;

    public Chat(Contacto contacto) {
        this.contacto = contacto;
        this.mensajes = new ArrayList<>();
    }

    public void agregarMensaje(Mensaje mensaje) {
        mensajes.add(mensaje);
    }

    public List<Mensaje> getMensajes() { return mensajes; }
    public Contacto getContacto() { return contacto; }
}
