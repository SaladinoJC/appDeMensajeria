package excepciones;

import mensajeria.Mensaje;

public class ExcepcionNoConectado extends Exception {

	private Mensaje mensaje;
	
	public ExcepcionNoConectado (Mensaje mensaje) {
		this.mensaje = mensaje;
	}
	
	
	public Mensaje getMensaje() {
		return this.mensaje;
	}
}
