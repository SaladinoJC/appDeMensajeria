package mensajeria;
import java.io.Serializable;
import java.util.*;
public class Mensaje implements Serializable{
	private String contenido;
	private String remitente;
	 private int puertoRemitente;
    private Date timestamp;

    public Mensaje(String contenido, String remitente, int puertoRemitente) {
        this.contenido = contenido;
        this.remitente = remitente;
        this.puertoRemitente = puertoRemitente;
        this.timestamp = new Date();
    }
    public int getPuertoRemitente() {
        return puertoRemitente;
    }
    public String getContenido() { return contenido; }
    public String getRemitente() { return remitente; }
    public Date getTimestamp() { return timestamp; }
}

