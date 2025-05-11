package mensajeria;
import java.io.Serializable;
import java.util.*;
public class Mensaje implements Serializable{
	private String contenido;
	private String nicknameRemitente;
	private int puertoRemitente;
    private Date timestamp;
    private String ipdestinatario;
    private String nicknameDestinatario;
    private int puertoDestinatario;

    public Mensaje(String contenido, String remitente, int puertoRemitente, String ipdestinatario, int puertoDestinatario, String nicknameDestinatario) {
        this.contenido = contenido;
        this.nicknameRemitente = remitente;
        this.ipdestinatario = ipdestinatario;
        this.puertoRemitente = puertoRemitente; 
        this.puertoDestinatario = puertoDestinatario;
        this.nicknameDestinatario = nicknameDestinatario;
        this.timestamp = new Date();
    }
    public int getPuertoRemitente() { return puertoRemitente;}    
    public int getPuertoDestinatario() { return puertoDestinatario;}
    public String getContenido() { return contenido; }
    public String getNicknameDestinatario() { return nicknameDestinatario; }
    public String getIpDestinatario() { return ipdestinatario; }
    public String getNicknameRemitente() { return nicknameRemitente; }
    public Date getTimestamp() { return timestamp; }
	@Override
	public String toString() {
		return "contenido= " + contenido + ", remitente= " + nicknameRemitente + ", puertoRemitente= " + puertoRemitente
				+", ipdestinatario= " + ipdestinatario + "]";
	}
    
    
}

