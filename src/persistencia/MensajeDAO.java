package persistencia;

import mensajeria.Mensaje;
import mensajeria.Contacto;
import java.util.List;

public interface MensajeDAO {
    void guardarMensajes(String chatId, List<Mensaje> mensajes) throws Exception;
    List<Mensaje> cargarMensajes(String chatId) throws Exception;
    
    void guardarContactos(List<Contacto> contactos) throws Exception;
    List<Contacto> cargarContactos() throws Exception;
}