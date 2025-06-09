package vistas;
import mensajeria.Observer;

public interface Observable {
    void agregarObservador(Observer o);
    void eliminarObservador(Observer o);
    void notificarObservadores(String tipo, Object dato);
}
