package servidor;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

public class MainServidorStandalone {
    public static void main(String[] args) {
        String nombre = null;
        int puertoControl = -1;

        // 1. CONSULTA AL MONITOR qué nombres y puertos están libres
        try (Socket sk = new Socket("localhost", 11000);
             ObjectOutputStream out = new ObjectOutputStream(sk.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(sk.getInputStream())) {
            out.writeObject("CONSULTA_LIBRES");
            List<String> libresNombres = (List<String>) in.readObject();
            List<Integer> libresPuertos = (List<Integer>) in.readObject();

            if (libresNombres.isEmpty() || libresPuertos.isEmpty()) {
                System.err.println("No hay nombres o puertos disponibles para nuevo servidor.");
                return; 
            }
            // Elige el menor nombre y puerto libre
            nombre = libresNombres.stream().sorted().findFirst().get();
            puertoControl = libresPuertos.stream().min(Integer::compareTo).get();
            System.out.println("Reserva: " + nombre + " puerto " + puertoControl);

        } catch (Exception e) {
            System.out.println("Error consultando libres al monitor: " + e.getMessage());
            return;
        }

        // 2. CREA el servidor y REGISTRA el par nombre/puerto al monitor
        try {
            Servidor servidor = new Servidor(nombre, puertoControl);

            try (Socket sk = new Socket("localhost", 11000);
                 ObjectOutputStream out = new ObjectOutputStream(sk.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(sk.getInputStream())) {
                out.writeObject("ALTA_SERVIDOR");
                out.writeObject(nombre);
                out.writeObject(puertoControl);
                String resp = (String) in.readObject();
                System.out.println("Registro de nuevo servidor: " + nombre + " (" + resp + ")");
            }
        } catch (Exception e) {
            System.out.println("No se pudo arrancar/registrar el nuevo servidor: " + e.getMessage());
        }
    }
}