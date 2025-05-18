package servidor;

import mensajeria.Mensaje;
import mensajeria.Usuario;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;
import java.awt.*;

public class Monitor {

    public static final String ROL_PRIMARIO = "PRIMARIO";
    public static final String ROL_SECUNDARIO = "SECUNDARIO";

    private final java.util.List<Integer> puertosControl;
    private final java.util.List<String> nombresServidores;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private int idxPrimario = 0;

    private Map<String, Usuario> ultimoUsuarios = new HashMap<>();
    private Map<String, LinkedList<Mensaje>> ultimoPendientes = new HashMap<>();

    private int puertoReplica = 10010;
    private int puertoAltas = 10003;
    private final Map<String, Integer> nombresApuertos = new LinkedHashMap<>();

    // Interfaz gráfica
    private final MonitorGUI gui;

    public Monitor(java.util.List<Integer> puertosControl) {
        this.puertosControl = puertosControl;
        this.nombresServidores = new ArrayList<>();

        // INICIALIZAR GUI
        gui = new MonitorGUI("Monitor");
        SwingUtilities.invokeLater(() -> gui.setVisible(true));

        for (int i = 0; i < puertosControl.size(); i++) {
            String nombre = "S" + i;
            this.nombresServidores.add(nombre);
            nombresApuertos.put(nombre, puertosControl.get(i));
        }
        new Thread(this::escucharReplicas).start();
        escucharAltasDinamicas();
        seleccionarRoles();
        iniciarMonitoreo();
    }

    /** Log seguro para multihilo. */
    private void appendLog(String msg) {
        String datetime = new java.text.SimpleDateFormat("HH:mm:ss").format(new Date());
        SwingUtilities.invokeLater(() -> gui.appendTexto("[" + datetime + "] " + msg + "\n"));
    }

    private void escucharReplicas() {
        try (ServerSocket ss = new ServerSocket(puertoReplica)) {
            appendLog("Monitor: Listo para recibir replicación de estado en " + puertoReplica);
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
            appendLog("Error en escucha de replica: " + e.getMessage());
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
            limpiarServidoresCaidos();
            if(!chequearPrimarioVivo()) promoverNuevoPrimario();
        }, 2, 2, TimeUnit.SECONDS);
    }

    private boolean chequearPrimarioVivo() {
        if (puertosControl.isEmpty()) return false;
        if (idxPrimario >= puertosControl.size()) return false;
        return enviarPing(idxPrimario);
    }

    private void limpiarServidoresCaidos() {
        for (int i = puertosControl.size() - 1; i >= 0; i--) {
            if (i == idxPrimario) continue;
            if (!enviarPing(i)) {
               // appendLog("Monitor: Liberando nombre y puerto de servidor caído (S" + i + ")");
                liberarNombrePuertoPorIndice(i);
            }
        }
    }

    private void promoverNuevoPrimario() {
    	int nuevoPrimario = -1;
    	for(int i = 0; i < puertosControl.size(); i++) {
    	    if(enviarPing(i)) {
    	        nuevoPrimario = i;
    	        break;
    	    }
    	}
    	if(nuevoPrimario != -1) {
    	    Integer puerto = puertosControl.get(nuevoPrimario);
    	    String nombreNuevoPrimario = null;
    	    for (Map.Entry<String, Integer> ent : nombresApuertos.entrySet()) {
    	        if (Objects.equals(ent.getValue(), puerto)) {
    	            nombreNuevoPrimario = ent.getKey(); break;
    	        }
    	    }
    	    if (nombreNuevoPrimario == null) nombreNuevoPrimario = "S?";
    	    appendLog("Monitor: Ascendiendo a " + nombreNuevoPrimario + " como PRIMARIO");
    	    idxPrimario = nuevoPrimario;
    	    enviarSincronizarEstado(idxPrimario, ultimoUsuarios, ultimoPendientes);
    	    seleccionarRoles();
    	} else {
    	    appendLog("Monitor: No hay servidores disponibles para promover");
    	}
    }

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
            appendLog("Monitor: Liberado " + nombreABorrar + " - " + puerto);
        }
        if (idxPrimario > idx) idxPrimario--;
        if (idxPrimario >= puertosControl.size()) idxPrimario = 0;
    }

    private void escucharAltasDinamicas() {
        new Thread(() -> {
            try (ServerSocket ss = new ServerSocket(puertoAltas)) {
                appendLog("Monitor: Esperando altas y consultas en " + puertoAltas);
                while (true) {
                    Socket sk = ss.accept();
                    ObjectInputStream in = new ObjectInputStream(sk.getInputStream());
                    ObjectOutputStream out = new ObjectOutputStream(sk.getOutputStream());
                    Object orden = in.readObject();

                    if ("CONSULTA_LIBRES".equals(orden)) {
                        java.util.List<String> usados = new ArrayList<>(nombresApuertos.keySet());
                        java.util.List<String> libresNombre = new ArrayList<>();
                        java.util.List<Integer> libresPuertos = new ArrayList<>();
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
                        appendLog("Monitor: Servidor ALTA: " + nombre + " en " + puertoCtrol);
                        enviarAsignarRol(puertosControl.size()-1, ROL_SECUNDARIO);
                    }
                    out.close();
                    in.close();
                    sk.close();
                }
            } catch (Exception e) {
                appendLog("Monitor: Error en altas dinámicas: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    // ---- CLASE INTERNA: GUI DE MONITOR (estilo Servidor) -----
    private static class MonitorGUI extends JFrame {
        private final JTextArea area;

        public MonitorGUI(String nombreVentana) {
            setTitle("Monitor Mensajería (" + nombreVentana + ")");
            setSize(350, 280);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setLayout(new BorderLayout());

            area = new JTextArea();
            area.setEditable(false);
            add(new JScrollPane(area), BorderLayout.CENTER);

            // igual que tu posicionamiento para servidores
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int screenWidth = screenSize.width;
            int screenHeight = screenSize.height;
            int numOffset = 0; // Si quieres desplazar varias, puedes variar este valor
            int x = screenWidth - getWidth() - 20 * numOffset;
            int y = (screenHeight - getHeight()) / 2;
            setLocation(x, y);

            setVisible(true);
        }

        public void appendTexto(String texto) {
            area.append(texto);
            area.setCaretPosition(area.getDocument().getLength());
        }
    }
}