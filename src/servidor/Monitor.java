package servidor;

import mensajeria.Mensaje;
import mensajeria.Usuario;

import java.util.*;
import java.util.concurrent.*;

public class Monitor {

    // Constantes de rol (iguales que en Servidor)
    public static final String ROL_PRIMARIO = "PRIMARIO";
    public static final String ROL_SECUNDARIO = "SECUNDARIO";

    private final List<Servidor> servidores;
    private int idxPrimario = 0;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // Estado de respaldo
    private Map<String, Usuario> ultimoUsuarios = new HashMap<>();
    private Map<String, LinkedList<Mensaje>> ultimoPendientes = new HashMap<>();

    public Monitor(List<Servidor> servidores) {
        this.servidores = servidores;
        seleccionarRoles();
        iniciarMonitoreo();
    }

    private void seleccionarRoles() {
        for(int i=0; i<servidores.size(); i++) {
            if(i == idxPrimario)
                servidores.get(i).asignarRol(ROL_PRIMARIO, this);
            else
                servidores.get(i).asignarRol(ROL_SECUNDARIO, this);
        }
    }

    private void iniciarMonitoreo() {
        scheduler.scheduleAtFixedRate(() -> {
            if(!chequearPrimarioVivo()) promoverNuevoPrimario();
        }, 2, 2, TimeUnit.SECONDS);
    }

    public void actualizarEstadoDesdePrimario(Servidor quien, Map<String, Usuario> usuarios, Map<String, LinkedList<Mensaje>> pendientes) {
        synchronized(this) {
            ultimoUsuarios = usuarios;
            ultimoPendientes = pendientes;
        }
        // Puedes loggear si lo deseas
    }

    private boolean chequearPrimarioVivo() {
        try {
            return servidores.get(idxPrimario).ping();
        } catch(Exception e) { return false; }
    }

    private void promoverNuevoPrimario() {
        int nuevoPrimario = -1;
        for(int i=1; i<=servidores.size(); i++) {
            int idx = (idxPrimario + i) % servidores.size();
            try {
                if(servidores.get(idx).ping()) {
                    nuevoPrimario = idx;
                    break;
                }
            } catch(Exception e) { }
        }
        if(nuevoPrimario != -1) {
            System.out.println("Monitor: Ascendiendo a "+servidores.get(nuevoPrimario).getNombreServidor()+" como PRIMARIO");
            idxPrimario = nuevoPrimario;
            servidores.get(idxPrimario).sincronizarEstado(ultimoUsuarios, ultimoPendientes);
            seleccionarRoles();
        } else {
            System.out.println("Monitor: No hay servidores disponibles para promover");
        }
    }

}