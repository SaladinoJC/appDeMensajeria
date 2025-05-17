package servidor;

import javax.swing.*;
import mensajeria.Mensaje;
import mensajeria.Usuario;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("serial")
public class Servidor extends JFrame {

    public static final String ROL_PRIMARIO = "PRIMARIO";
    public static final String ROL_SECUNDARIO = "SECUNDARIO";

    private JTextArea logArea;

    private final Map<String, Usuario> directorioUsuarios = new HashMap<>();
    private final Map<String, LinkedList<Mensaje>> MapPendientes = new HashMap<>();
    private int puertoMensajes = 10000;
    private int puertoRegistros = 10001;
    private int puertoDirectorio = 10002;

    private ServerSocket serverSocketRegistros;
    private ServerSocket serverSocketMensajes;
    private ServerSocket serverSocketDirectorio;

    private Thread hiloRegistros;
    private Thread hiloMensajes;
    private Thread hiloDirectorio;
    private Thread hiloReplicaEstado;

    private String rol = ROL_SECUNDARIO;
    private Monitor monitor;
    private String nombreServidor; // Para identificación en logs
    private final AtomicBoolean activo = new AtomicBoolean(false);

    // Simula vida real del servidor
    private volatile boolean estaVivo = true;

    public Servidor(String nombreServidor) {
        this.nombreServidor = nombreServidor;
        setTitle("Servidor Mensajería ("+nombreServidor+")");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        logArea = new JTextArea();
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = screenSize.width;
        int screenHeight = screenSize.height;
        int x = screenWidth - getWidth() - 20 * Integer.parseInt(nombreServidor.replaceAll("\\D","")); // Separar ventanas
        int y = (screenHeight - getHeight()) / 2;
        setLocation(x, y);
        setVisible(true);

        log("Arrancó " + nombreServidor + " en modo SECUNDARIO");

        // Listener para apagar realmente el servidor al cerrar ventana
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                shutdown();
            }
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                shutdown();
            }
        });
    }

    // Llamado por Monitor para cambiar a PRIMARIO/SECUNDARIO
    public synchronized void asignarRol(String nuevoRol, Monitor monitor) {
        this.monitor = monitor;
        if(!this.rol.equals(nuevoRol)) {
            this.rol = nuevoRol;

            if(rol.equals(ROL_PRIMARIO)) {
                log("*** Ahora soy PRIMARIO ***");
                iniciarServidores();
                iniciarReplicaEstadoConMonitor();
            } else {
                log("*** Ahora soy SECUNDARIO ***");
                pararServidores();
            }
        }
    }

    private void iniciarServidores() {
        if (!estaVivo) return;      // Si ya está muerto, no iniciar nada
        if (activo.get()) return;   // Ya estaba activo

        activo.set(true);

        hiloMensajes = new Thread(() -> iniciarServidorMensajes(puertoMensajes));
        hiloRegistros = new Thread(() -> iniciarServidorRegistros(puertoRegistros));
        hiloDirectorio = new Thread(() -> iniciarServidorDirectorio(puertoDirectorio));
        hiloMensajes.start();
        hiloRegistros.start();
        hiloDirectorio.start();
    }

    private void pararServidores() {
        activo.set(false);
        try { if (serverSocketMensajes != null) serverSocketMensajes.close(); } catch (Exception e) { }
        try { if (serverSocketRegistros != null) serverSocketRegistros.close(); } catch (Exception e) { }
        try { if (serverSocketDirectorio != null) serverSocketDirectorio.close(); } catch (Exception e) { }
    }

    private void iniciarServidorMensajes(int puerto) {
        try {
            serverSocketMensajes = new ServerSocket(puerto);
            log("Servidor de Mensajes iniciado en el puerto " + puerto);
            while (activo.get() && rol.equals(ROL_PRIMARIO) && estaVivo) {
                Socket socketMensajes = serverSocketMensajes.accept();
                ObjectInputStream in = new ObjectInputStream(socketMensajes.getInputStream());
                Mensaje mensaje = (Mensaje) in.readObject();
                manejarMensajes(mensaje);
            }
        } catch (Exception e) {
            if (activo.get() && rol.equals(ROL_PRIMARIO) && estaVivo)
                log("Error servidor de mensajes: " + e.getMessage());
        }
    }

    private void iniciarServidorRegistros(int puerto) {
        try {
            serverSocketRegistros = new ServerSocket(puerto);
            log("Servidor de registros iniciado en el puerto " + puerto);
            while (activo.get() && rol.equals(ROL_PRIMARIO) && estaVivo) {
                Socket socketRegistros = serverSocketRegistros.accept();
                ObjectInputStream in = new ObjectInputStream(socketRegistros.getInputStream());
                Usuario usuario = (Usuario) in.readObject();
                manejarRegistros(usuario);
            }
        } catch (Exception e) {
            if (activo.get() && rol.equals(ROL_PRIMARIO) && estaVivo)
                log("Error servidor de registros: " + e.getMessage());
        }
    }

    private void iniciarServidorDirectorio(int puerto) {
        try {
            serverSocketDirectorio = new ServerSocket(puerto);
            log("Servidor de Directorio iniciado en el puerto " + puerto);
            while (activo.get() && rol.equals(ROL_PRIMARIO) && estaVivo) {
                Socket socketRecibePedido = serverSocketDirectorio.accept();
                ObjectOutputStream out = new ObjectOutputStream(socketRecibePedido.getOutputStream());
                out.flush();
                synchronized(this) {
                    out.writeObject(this.directorioUsuarios);
                }
                out.close();
            }
        } catch (IOException e) {
            if (activo.get() && rol.equals(ROL_PRIMARIO) && estaVivo)
                log("Error al iniciar el servidor de directorio: " + e.getMessage());
        }
    }

    private void manejarMensajes(Mensaje mensaje) {
        new Thread(() -> {
            Socket socket2 = null;
            ObjectOutputStream out = null;
            try {
                socket2 = new Socket(mensaje.getIpDestinatario(), mensaje.getPuertoDestinatario());
                out = new ObjectOutputStream(socket2.getOutputStream());
                out.flush();
                out.writeObject(mensaje);
            } catch (IOException e) {
                synchronized (this) {
                    this.MapPendientes
                      .computeIfAbsent(mensaje.getNicknameDestinatario(), k -> new LinkedList<>())
                      .addLast(mensaje);
                }
            } finally {
                try { if (out != null) out.close(); } catch (Exception e) {}
                try { if (socket2 != null && !socket2.isClosed()) socket2.close(); } catch (Exception e) {}
            }
        }).start();
    }

    private void manejarRegistros(Usuario usuario) {
        String nickname = usuario.getNickname();
        synchronized(this) {
            if (!directorioUsuarios.containsKey(nickname)) {
                directorioUsuarios.put(nickname, usuario);
                log("Usuario registrado y logueado: " + nickname + ", en el puerto " + usuario.getPuerto());
                this.MapPendientes.put(nickname, new LinkedList<Mensaje>());
            } else {
                log("Usuario Logueado: " + nickname + ", en el puerto " + usuario.getPuerto());
                enviarMensajesPendientes(nickname);
            }
        }
    }

    private void enviarMensajesPendientes(String nickname) {
        LinkedList<Mensaje> pendientes = MapPendientes.get(nickname);
        if (pendientes == null) return;
        synchronized (pendientes) {
            while (!pendientes.isEmpty()) {
                Mensaje mensaje = pendientes.getFirst();
                Socket socket2 = null;
                ObjectOutputStream out = null;
                try {
                    socket2 = new Socket(mensaje.getIpDestinatario(), mensaje.getPuertoDestinatario());
                    out = new ObjectOutputStream(socket2.getOutputStream());
                    out.flush();
                    out.writeObject(mensaje);
                } catch (IOException e) {
                    log("Error al enviar mensaje pendiente");
                } finally {
                    try { if (out != null) out.close(); } catch (Exception e) {}
                    try { if (socket2 != null && !socket2.isClosed()) socket2.close(); } catch (Exception e) {}
                }
                pendientes.removeFirst();
            }
        }
    }

    private void log(String mensaje) {
        SwingUtilities.invokeLater(() -> logArea.append(mensaje + "\n"));
    }

    // ***** REPORTE/SYNC AL MONITOR *****

    private void iniciarReplicaEstadoConMonitor() {
        if (hiloReplicaEstado != null && hiloReplicaEstado.isAlive()) return;
        hiloReplicaEstado = new Thread(() -> {
            while (rol.equals(ROL_PRIMARIO) && monitor != null && estaVivo) {
                try {
                    Map<String, Usuario> copiaUsuarios;
                    Map<String, LinkedList<Mensaje>> copiaPendientes;
                    synchronized(this) {
                        copiaUsuarios = new HashMap<>(directorioUsuarios);
                        copiaPendientes = new HashMap<>();
                        for (Map.Entry<String, LinkedList<Mensaje>> ent : MapPendientes.entrySet()) {
                            copiaPendientes.put(ent.getKey(), new LinkedList<>(ent.getValue()));
                        }
                    }
                    monitor.actualizarEstadoDesdePrimario(this, copiaUsuarios, copiaPendientes);
                    Thread.sleep(3000);
                } catch (Exception e) {
                    // ciclo sigue
                }
            }
        });
        hiloReplicaEstado.start();
    }

    // Llamado solo por el Monitor cuando asciende este servidor a primario
    public synchronized void sincronizarEstado(Map<String, Usuario> usuarios, Map<String, LinkedList<Mensaje>> pendientes) {
        directorioUsuarios.clear();
        directorioUsuarios.putAll(usuarios);
        MapPendientes.clear();
        for (Map.Entry<String, LinkedList<Mensaje>> ent : pendientes.entrySet()) {
            MapPendientes.put(ent.getKey(), new LinkedList<>(ent.getValue()));
        }
        log("Estado sincronizado desde Monitor tras ascenso a PRIMARIO");
    }

    // Llamado por el Monitor para chequear si está vivo
    public boolean ping() {
        if (!estaVivo) throw new RuntimeException("Servidor caído");
        return true;
    }

    public void shutdown() {
        estaVivo = false;
        pararServidores();
        log("Servidor " + nombreServidor + " APAGADO por cierre de ventana");
    }

    public String getNombreServidor() {
        return nombreServidor;
    }
}