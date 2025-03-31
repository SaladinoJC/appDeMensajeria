package appDeMensajeria;

public class Contacto {
	 private String nombre;
	    private String direccionIP;
	    private int puerto;

	    public Contacto(String nombre, String direccionIP, int puerto) {
	        this.nombre = nombre;
	        this.direccionIP = direccionIP;
	        this.puerto = puerto;
	    }

	    public String getNombre() { return nombre; }
	    public String getDireccionIP() { return direccionIP; }
	    public int getPuerto() { return puerto; }
}
