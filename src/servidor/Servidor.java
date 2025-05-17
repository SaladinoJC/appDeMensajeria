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

    // Puerto de control
    private int puertoControl;
    private ServerSocket serverSocketControl;
    private Thread hiloControl;

    public Servidor(String nombreServidor, int puertoControl) {
        this.nombreServidor = nombreServidor;
        this.puertoControl = puertoControl;
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

        iniciarServidorControl();
    }

    // Este método solamente es usado para inyección inicial manual (no por socket)
    public synchronized void asignarRol(String nuevoRol, Monitor monitor) {
        this.monitor = monitor;
        if(!this.rol.equals(nuevoRol)) {
            this.rol = nuevoRol;
            if(rol.equals(ROL_PRIMARIO)) {
                log("*** Ahora soy PRIMARIO ***");
                iniciarServidores();
                //iniciarReplicaEstadoConMonitor(); NO hace falta la replica periódica
            } else {
                log("*** Ahora soy SECUNDARIO ***");
                pararServidores();
            }
        }
    }

    private synchronized void asignarRolDesdeSocket(String nuevoRol) {
        if (!this.rol.equals(nuevoRol)) {
            this.rol = nuevoRol;
            if (rol.equals(ROL_PRIMARIO)) {
                log("*** Ahora soy PRIMARIO ***");
                iniciarServidores();
                //iniciarReplicaEstadoConMonitor();
            } else {
                log("*** Ahora soy SECUNDARIO ***");
                pararServidores();
            }
        }
    }

    private void iniciarServidorControl() {
        hiloControl = new Thread(() -> {
            try {
                serverSocketControl = new ServerSocket(puertoControl);
                log("Servidor de CONTROL en puerto " + puertoControl);
                while (estaVivo) {
                    Socket socket = serverSocketControl.accept();
                    ObjectInputStream  in  = new ObjectInputStream(socket.getInputStream());
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    Object orden = in.readObject();
                    if (orden instanceof String) {
                        switch((String)orden) {
                            case "ASIGNAR_ROL":
                                String nuevorol = (String) in.readObject();
                                asignarRolDesdeSocket(nuevorol);
                                out.writeObject("OK");
                                break;
                            case "PING":
                                out.writeObject("OK");
                                break;
                            case "SINCRONIZAR_ESTADO":
                                Map<String, Usuario> usuarios = (Map<String, Usuario>) in.readObject();
                                Map<String, LinkedList<Mensaje>> pendientes = (Map<String, LinkedList<Mensaje>>) in.readObject();
                                sincronizarEstado(usuarios, pendientes);
                                out.writeObject("OK");
                                break;
                            default:
                                out.writeObject("ERROR");
                        }
                    }
                    out.close(); in.close(); socket.close();
                }
            } catch (IOException | ClassNotFoundException e) {
                log("Error en socket control: "+e.getMessage());
            }
        });
        hiloControl.start();
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
                    replicaEstadoAlMonitor();
                }
            } finally {
                try { if (out != null) out.close(); } catch (Exception e) {}
                try { if (socket2 != null && !socket2.isClosed()) socket2.close(); } catch (Exception e) {}
            }
        }).start();
    }

    private void manejarRegistros(Usuario usuario) {
        String nickname = usuario.getNickname();
        boolean modificado = false;
        synchronized(this) {
            if (!directorioUsuarios.containsKey(nickname)) {
                directorioUsuarios.put(nickname, usuario);
                log("Usuario registrado y logueado: " + nickname + ", en el puerto " + usuario.getPuerto());
                this.MapPendientes.put(nickname, new LinkedList<Mensaje>());
                modificado = true;
            } else {
                log("Usuario Logueado: " + nickname + ", en el puerto " + usuario.getPuerto());
                enviarMensajesPendientes(nickname);
            }
        }
        if (modificado) {
            replicaEstadoAlMonitor();
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

    // -- RÉPLICA AL MONITOR IMMEDIATA --
    private void replicaEstadoAlMonitor() {
        try (Socket s = new Socket("localhost", 10010);
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream())) {
            out.writeObject("REPLICA_ESTADO");
            synchronized(this) {
                out.writeObject(new HashMap<>(directorioUsuarios));
                Map<String, LinkedList<Mensaje>> copiaPendientes = new HashMap<>();
                for (Map.Entry<String, LinkedList<Mensaje>> ent : MapPendientes.entrySet()) {
                    copiaPendientes.put(ent.getKey(), new LinkedList<>(ent.getValue()));
                }
                out.writeObject(copiaPendientes);
            }
        } catch (Exception e) {
            log("No se pudo replicar estado al monitor ("+e.getMessage()+")");
        }
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

    // Si se llama por referencia (pruebas locales)
    public boolean ping() {
        if (!estaVivo) throw new RuntimeException("Servidor caído");
        return true;
    }

    public void shutdown() {
        estaVivo = false;
        pararServidores();
        try { if (serverSocketControl != null) serverSocketControl.close(); } catch (Exception e) { }
        log("Servidor " + nombreServidor + " APAGADO por cierre de ventana");
    }

    public String getNombreServidor() {
        return nombreServidor;
    }
}