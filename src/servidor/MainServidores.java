package servidor;

import java.util.*;

public class MainServidores {
    public static void main(String[] args) {
        List<Servidor> servidores = new ArrayList<>();
        for(int i=0; i<4; i++) {
            Servidor s = new Servidor("S" + i);
            servidores.add(s);
        }
        Monitor monitor = new Monitor(servidores);
        // Listo, todo ocurre internamente.
    }
}