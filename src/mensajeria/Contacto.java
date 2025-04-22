package mensajeria;

public class Contacto {
	 	private String nombre;
	    private String direccionIP;
	    private int puerto;
	    private String nickname;
	    
	    public Contacto(String nombre, String direccionIP, int puerto, String nickname) {
	        this.nombre = nombre;
	        this.direccionIP = direccionIP;
	        this.puerto = puerto;
	        this.nickname = nickname;
	    }

	    public String getNombre() { return nombre; }
	    public String getDireccionIP() { return direccionIP; }
	    public String getNickname() { return nickname; }
	    public int getPuerto() { return puerto; }
}
