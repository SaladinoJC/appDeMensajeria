package servidor;

import mensajeria.Mensaje;
import mensajeria.Usuario;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class Monitor {

    public static final String ROL_PRIMARIO = "PRIMARIO";
    public static final String ROL_SECUNDARIO = "SECUNDARIO";

    private final List<Integer> puertosControl;
    private final List<String> nombresServidores;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private int idxPrimario = 0;

    private Map<String, Usuario> ultimoUsuarios = new HashMap<>();
    private Map<String, LinkedList<Mensaje>> ultimoPendientes = new HashMap<>();

    private int puertoReplica = 10010;
    private int puertoAltas = 11000;

    // NUEVO: para mapear nombre->puerto (rango S0-S9 y 10004-10013)
    private final Map<String, Integer> nombresApuertos = new LinkedHashMap<>();

    public Monitor(List<Integer> puertosControl) {
        this.puertosControl = puertosControl;
        this.nombresServidores = new ArrayList<>();
        for (int i = 0; i < puertosControl.size(); i++) {
            String nombre = "S" + i;
            this.nombresServidores.add(nombre);
            nombresApuertos.put(nombre, puertosControl.get(i));
        }
        new Thread(this::escucharReplicas).start();
        escucharAltasDinamicas(); // llamada aquí
        seleccionarRoles();
        iniciarMonitoreo();
    }

    private void escucharReplicas() {
        try (ServerSocket ss = new ServerSocket(puertoReplica)) {
            System.out.println("Monitor: Listo para recibir replicación de estado en " + puertoReplica);
            while (true) {
                Socket sock = ss.accept();
                ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
                Object orden = ois.readObject();
                if ("REPLICA_ESTADO".equals(orden)) {
                    Map<String, Usuario> usuarios = (Map<String, Usuario>) ois.readObject();
                    Map<String, LinkedList<Mensaje>> pendientes = (Map<String, LinkedList<Mensaje>>) ois.readObject();
                    synchronized(this) {
                        this.ultimoUsuarios = usuarios;
                        this.ultimoPendientes = pendientes;
                    }
                }
                ois.close();
                sock.close();
            }
        } catch (Exception e) {
            System.out.println("Error en escucha de replica: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean enviarAsignarRol(int idx, String rol) {
        try (Socket sock = new Socket("localhost", puertosControl.get(idx));
             ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
             ObjectInputStream  in  = new ObjectInputStream(sock.getInputStream())) {
            out.writeObject("ASIGNAR_ROL");
            out.writeObject(rol);
            String resp = (String) in.readObject();
            return "OK".equals(resp);
        } catch (Exception e) { return false; }
    }

    private boolean enviarAsignarRolPorNombre(String nombre, String rol) {
        Integer idx = nombresApuertos.containsKey(nombre) ? puertosControl.indexOf(nombresApuertos.get(nombre)) : -1;
        if(idx == -1) return false;
        return enviarAsignarRol(idx, rol);
    }

    private boolean enviarPing(int idx) {
        try (Socket sock = new Socket("localhost", puertosControl.get(idx));
             ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
             ObjectInputStream  in  = new ObjectInputStream(sock.getInputStream())) {
            out.writeObject("PING");
            String resp = (String) in.readObject();
            return "OK".equals(resp);
        } catch (Exception e) { return false; }
    }

    private boolean enviarSincronizarEstado(int idx, Map<String, Usuario> usuarios, Map<String, LinkedList<Mensaje>> pendientes) {
        try (Socket sock = new Socket("localhost", puertosControl.get(idx));
             ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
             ObjectInputStream  in  = new ObjectInputStream(sock.getInputStream())) {
            out.writeObject("SINCRONIZAR_ESTADO");
            out.writeObject(usuarios);
            out.writeObject(pendientes);
            String resp = (String) in.readObject();
            return "OK".equals(resp);
        } catch (Exception e) { return false; }
    }

    private void seleccionarRoles() {
        for(int i=0;i<puertosControl.size();i++) {
            if(i == idxPrimario)
                enviarAsignarRol(i, ROL_PRIMARIO);
            else
                enviarAsignarRol(i, ROL_SECUNDARIO);
        }
    }

    private void iniciarMonitoreo() {
        scheduler.scheduleAtFixedRate(() -> {
            limpiarServidoresCaidos(); // <-- LLAMADA AQUÍ!
            if(!chequearPrimarioVivo()) promoverNuevoPrimario();
        }, 2, 2, TimeUnit.SECONDS);
    }

    private boolean chequearPrimarioVivo() {
        if (puertosControl.isEmpty()) return false;
        if (idxPrimario >= puertosControl.size()) return false;
        return enviarPing(idxPrimario);
    }

    /**
     * Libera los servidores que ya no responden al PING (todos los secundarios, no el primario!).
     * Muy importante: se llama antes de promover para no dejar huecos y liberar slots.
     */
    private void limpiarServidoresCaidos() {
        // Empieza desde el último al primero para evitar problemas al remover mientras se itera
        for (int i = puertosControl.size() - 1; i >= 0; i--) {
            if (i == idxPrimario) continue; // El primario se chequea y promueve aparte
            if (!enviarPing(i)) {
                System.out.println("Monitor: Liberando nombre y puerto de servidor caído (S" + i + ")");
                liberarNombrePuertoPorIndice(i);
            }
        }
    }

    /**
     * Cuando se detecta que el primario está caído y se promueve otro, hay que liberar ese nombre/puerto.
     */
    private void promoverNuevoPrimario() {
        // Liberar el primario caído ANTES de promover (idxPrimario)
        System.out.println("Monitor: Liberando primario caído (S" + idxPrimario + ")");
        liberarNombrePuertoPorIndice(idxPrimario);

        if(puertosControl.isEmpty()) {
            System.out.println("Monitor: No hay servidores disponibles para promover");
            return;
        }

        int nuevoPrimario = -1;
        for(int i = 0; i < puertosControl.size(); i++) {
            if(enviarPing(i)) {
                nuevoPrimario = i;
                break;
            }
        }
        if(nuevoPrimario != -1) {
            System.out.println("Monitor: Ascendiendo a S"+nuevoPrimario+" como PRIMARIO");
            idxPrimario = nuevoPrimario;
            enviarSincronizarEstado(idxPrimario, ultimoUsuarios, ultimoPendientes);
            seleccionarRoles();
        } else {
            System.out.println("Monitor: No hay servidores disponibles para promover");
        }
    }

    /**
     * Elimina nombreServidor/puerto/entrada de los registros internos en función del índice actual.
     */
    private void liberarNombrePuertoPorIndice(int idx) {
        if (idx < 0 || idx >= puertosControl.size()) return;
        Integer puerto = puertosControl.remove(idx);
        String nombreABorrar = null;
        for (Map.Entry<String, Integer> ent : nombresApuertos.entrySet()) {
            if (Objects.equals(ent.getValue(), puerto)) {
                nombreABorrar = ent.getKey(); break;
            }
        }
        if (nombreABorrar != null) {
            nombresApuertos.remove(nombreABorrar);
            nombresServidores.remove(nombreABorrar);
            System.out.println("Monitor: Liberado " + nombreABorrar + " - " + puerto);
        }
        // Si el primario fue removido, hay que ajustar el índice del primario (idxPrimario)
        if (idxPrimario > idx) idxPrimario--;
        if (idxPrimario >= puertosControl.size()) idxPrimario = 0;
    }

    //------------------ ALTA/INFO DE NUEVOS SERVIDORES DINÁMICAMENTE ------------------

    private void escucharAltasDinamicas() {
        new Thread(() -> {
            try (ServerSocket ss = new ServerSocket(puertoAltas)) {
                System.out.println("Monitor: Esperando altas y consultas en " + puertoAltas);
                while (true) {
                    Socket sk = ss.accept();
                    ObjectInputStream in = new ObjectInputStream(sk.getInputStream());
                    ObjectOutputStream out = new ObjectOutputStream(sk.getOutputStream());
                    Object orden = in.readObject();

                    if ("CONSULTA_LIBRES".equals(orden)) {
                        // Devuelve nombres-libres y puertos-libres (rangos fijos S0-S9, 10004-10013)
                        List<String> usados = new ArrayList<>(nombresApuertos.keySet());
                        List<String> libresNombre = new ArrayList<>();
                        List<Integer> libresPuertos = new ArrayList<>();

                        for (int i = 0; i < 10; i++) {
                            String posible = "S" + i;
                            if (!usados.contains(posible)) libresNombre.add(posible);
                        }
                        for (int p = 10004; p <= 10013; p++) {
                            if (!nombresApuertos.containsValue(p)) libresPuertos.add(p);
                        }
                        out.writeObject(libresNombre);
                        out.writeObject(libresPuertos);

                    } else if ("ALTA_SERVIDOR".equals(orden)) {
                        String nombre = (String) in.readObject();
                        int puertoCtrol = (Integer) in.readObject();
                        nombresApuertos.put(nombre, puertoCtrol);
                        puertosControl.add(puertoCtrol);
                        if (!nombresServidores.contains(nombre)) nombresServidores.add(nombre);
                        out.writeObject("OK");
                        System.out.println("Monitor: Servidor ALTA: " + nombre + " en " + puertoCtrol);
                        enviarAsignarRol(puertosControl.size()-1, ROL_SECUNDARIO);
                    }
                    out.close();
                    in.close();
                    sk.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}