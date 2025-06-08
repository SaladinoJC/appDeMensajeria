package servidor;
import java.util.*;

public class MainServidores {
    public static void main(String[] args) {
        List<Servidor> servidores = new ArrayList<>();
        List<Integer> puertosControl = new ArrayList<>();
        for(int i=0; i<1; i++) {
            int puertoControl = 10004 + i;
            Servidor s = new Servidor("S" + i, puertoControl);
            servidores.add(s);
            puertosControl.add(puertoControl);
        }
        Monitor monitor = new Monitor(puertosControl);
    }
}