package mensajeria;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import mensajeria.Usuario;

public class Servidor extends JFrame {

    private JTextArea logArea;
    private final Map<String, Usuario> directorioUsuarios = new HashMap<>();
    private final Map<String, LinkedList <Mensaje>> MapPendientes = new HashMap<>();
    private int puertoMensajes = 10000;
    private int puertoRegistros = 10001;
    private int puertoDirectorio = 10002;

    private ServerSocket serverSocketRegistros;
    private ServerSocket serverSocketMensajes;
    private ServerSocket serverSocketDirectorio;

    public Servidor() {
    	
        setTitle("Servidor de Mensajería");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        logArea = new JTextArea();
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        setVisible(true);
        
        
        // Iniciar el servidor en un hilo aparte
        new Thread(() -> iniciarServidorRegistros(puertoRegistros)).start();
        new Thread(() -> iniciarServidorMensajes(puertoMensajes)).start();
        new Thread(() -> iniciarServidorDirectorio(puertoDirectorio)).start();
    }

    private void iniciarServidorRegistros(int puerto) {
        try {
            serverSocketRegistros = new ServerSocket(puerto);
            log("Servidor de registros iniciado en el puerto " + puerto);
            while (true) {
                Socket socketCliente = serverSocketRegistros.accept();
                ObjectInputStream in = new ObjectInputStream(socketCliente.getInputStream());
                Usuario usuario = (Usuario) in.readObject();
                manejarRegistros(usuario);
            }
        } catch (Exception e) {
            log("Error al iniciar el servidor: " + e.getMessage());
        }
    }
    
    private void iniciarServidorMensajes(int puerto) {
    	try {
            serverSocketMensajes = new ServerSocket(puerto); 
            log("Servidor de Mensajes iniciado en el puerto " + puerto);
            while (true) {
                Socket socketCliente = serverSocketMensajes.accept();
                ObjectInputStream in = new ObjectInputStream(socketCliente.getInputStream());
        		Mensaje mensaje = (Mensaje) in.readObject();
                manejarMensajes(mensaje);
                socketCliente.close();
            }
        } catch (IOException | ClassNotFoundException e) {
            log("Error al iniciar el servidor: " + e.getMessage());
        }
    }
    private void iniciarServidorDirectorio(int puerto) {}

    
    private void manejarMensajes(Mensaje mensaje) {
    	try {
            Socket socket2 = new Socket(mensaje.getIpDestinatario(), mensaje.getPuertoDestinatario());
            ObjectOutputStream out = new ObjectOutputStream(socket2.getOutputStream());
            out.flush();
            out.writeObject(mensaje);
            out.close();
            log("llego el mensaje" + mensaje.getContenido());
        } catch (IOException e) {
        		this.MapPendientes.get(mensaje.getNicknameDestinatario()).addLast(mensaje);
        }
     
    	
    }
    
    private void manejarRegistros(Usuario usuario) {
         {
            // Leer el primer mensaje que contiene la información del cliente
            String nickname = usuario.getNickname();
                // Si el remitente está registrado en la lista de conectados, no es un registro, sino un mensaje
                if (!directorioUsuarios.containsKey(nickname)) {
                    // Registrar al cliente como conectado
                    directorioUsuarios.put(nickname, usuario);
                    log("Usuario registrado: " + nickname);
                    this.MapPendientes.put(nickname, new LinkedList<Mensaje>());
                } else {
                    log("Cliente ya registrado: " + nickname);
                    enviarMensajesPendientes(nickname);
                }
            } 
        } 
            
     

    private void enviarMensajesPendientes(String nickname) {
            LinkedList<Mensaje> pendientes = MapPendientes.get(nickname);
            while (!pendientes.isEmpty()) {
            	Mensaje mensaje = pendientes.getFirst();
            	log("el mensaje sacado de la lista es" + mensaje.getContenido());
                
                          
                try {
                    Socket socket2 = new Socket(mensaje.getIpDestinatario(), mensaje.getPuertoDestinatario());
                    ObjectOutputStream out = new ObjectOutputStream(socket2.getOutputStream());
                    out.flush();
                    out.writeObject(mensaje);
                    out.close();
                    log("llego el mensaje" + mensaje.getContenido());
                } catch (IOException e) {
                		this.MapPendientes.get(mensaje.getNicknameDestinatario()).addLast(mensaje);
                }
                try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

                pendientes.removeFirst();
            }
       
    }

    private void log(String mensaje) {
        SwingUtilities.invokeLater(() -> logArea.append(mensaje + "\n"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Servidor());
    }
}
