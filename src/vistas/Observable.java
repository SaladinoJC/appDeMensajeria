package vistas;

public interface Observable {
    void agregarObserver(Observer o);
    void eliminarObserver(Observer o);
    void notificarObservers(Object evento);
}