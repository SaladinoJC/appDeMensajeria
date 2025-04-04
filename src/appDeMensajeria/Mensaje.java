package appDeMensajeria;
import java.io.Serializable;
import java.util.*;
public class Mensaje implements Serializable{
	private String contenido;
	private String remitente;
    private Date timestamp;

    public Mensaje(String contenido, String remitente) {
        this.contenido = contenido;
        this.remitente = remitente;
        this.timestamp = new Date();
    }

    public String getContenido() { return contenido; }
    public String getRemitente() { return remitente; }
    public Date getTimestamp() { return timestamp; }
}

