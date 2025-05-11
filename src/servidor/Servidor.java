package servidor;

import javax.swing.*;

import mensajeria.Mensaje;
import mensajeria.Usuario;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;

@SuppressWarnings("serial")
public class Servidor extends JFrame {

    private JTextArea logArea;

    private final Map<String, Usuario> directorioUsuarios = new HashMap<>();
    private final Map<String, LinkedList<Mensaje>> MapPendientes = new HashMap<>();
    private boolean primario = false; 
    private boolean secundario = false;
    private int puertoEstado = 10005;
    private int puertoSincronizacion = 10008;
    private int puertoMensajes = 10000;
    private int puertoRegistros = 10001;
    private int puertoDirectorio = 10002;
    private ServerSocket serverSocketRegistros;
    private ServerSocket serverSocketMensajes;
    private ServerSocket serverSocketDirectorio;
    private ServerSocket serverSocketSincronizacion;
    private ServerSocket serverSocketMonitor;
    private Thread sincronizacionDeDatos;

    public Servidor() {
        setTitle("Servidor de Mensajería");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        logArea = new JTextArea();
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = screenSize.width;
        int screenHeight = screenSize.height;
        int x = screenWidth - getWidth(); // A la derecha
        int y = (screenHeight - getHeight()) / 2; // Centrado verticalmente
        setLocation(x, y);
        //setVisible(true);
        
        registrarseAlMonitor();
        new Thread(() ->iniciarEscuchaAlMonitor(puertoEstado)).start();
        
        // Inicia los servidores en hilos aparte
        /*if(this.primario) {
        	 new Thread(() -> iniciarServidorMensajes(puertoMensajes)).start();
             new Thread(() -> iniciarServidorRegistros(puertoRegistros)).start();
             new Thread(() -> iniciarServidorDirectorio(puertoDirectorio)).start();
             //new Thread(() -> iniciarRecepcionDeSincronizacionDeDatos(puertoSincronizacion)).start();
             new Thread(() ->iniciarEscuchaAlMonitor(puertoEstado)).start();
        }*/
    }
    
    private void resincronizacionDeEstado() {
    	//Se deben actualizar tanto el directorio de usuarios y el mapa de pendientes del servidor que se volvio a reconectar
    }
    
    

    private void EnvioDeSincronizacionDeDatos(int puertoSincronizacion, Mensaje mensajePendianteAEnviar, Usuario usuarioAEnviar) {
    	Socket socket;
    	try {
			socket = new Socket("localhost", 10009);
			 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
	         out.flush();
	         if(mensajePendianteAEnviar.equals(null))
	        	 out.writeObject(usuarioAEnviar);
	         else
	        	 out.writeObject(mensajePendianteAEnviar);
	         out.close();
	         socket.close();
		}
    	
    	 catch (Exception e) {
  		}
	}
    
    
    private void iniciarRecepcionDeSincronizacionDeDatos(int puertoSincronizacion) {
    	try {
    		serverSocketSincronizacion = new ServerSocket(puertoSincronizacion);
    		while (true) {
    		 Socket socketSincronizacion = serverSocketRegistros.accept();
   			 ObjectInputStream input = new ObjectInputStream(socketSincronizacion.getInputStream());
   			 Object objetoRecibido = input.readObject();
   			 
   			if (objetoRecibido instanceof Mensaje) {
   				Mensaje mensajePendienteNuevo = (Mensaje) objetoRecibido;
   				
   				synchronized (this) {
                    this.MapPendientes.get(mensajePendienteNuevo.getNicknameDestinatario()).addLast(mensajePendienteNuevo);
                }
   			} 
   			else 
   				if (objetoRecibido instanceof Usuario) {
   					Usuario usuarioNuevo = (Usuario) objetoRecibido;
   					
   			        if (!directorioUsuarios.containsKey(usuarioNuevo.getNickname())) {
   			            directorioUsuarios.put(usuarioNuevo.getNickname(), usuarioNuevo);
   			            this.MapPendientes.put(usuarioNuevo.getNickname(), new LinkedList<Mensaje>());
   			        }
   				}
   			
   			socketSincronizacion.close();
   			
    		}
		}
    	
    	 catch (Exception e) {
  		}
    }
    
    
    private void iniciarEscuchaAlMonitor(int puertoEstado) {
    	try {
    		serverSocketMonitor = new ServerSocket(puertoEstado);
    		while (true) {
    		 Socket socketEstado = serverSocketRegistros.accept();
   			 ObjectInputStream input = new ObjectInputStream(socketEstado.getInputStream());
   			 String estadoMonitor = input.readLine();
   			 
   			 if(estadoMonitor.equals("primario")) {
   				if(!this.primario) {
   					setVisible(true);
   	   				new Thread(() -> iniciarServidorMensajes(puertoMensajes)).start();
   	                new Thread(() -> iniciarServidorRegistros(puertoRegistros)).start();
   	                new Thread(() -> iniciarServidorDirectorio(puertoDirectorio)).start();
   	                this.primario = true;
   	                
   	                if(this.secundario) {
   	                	this.sincronizacionDeDatos.stop(); //Ver si funciona, es para detener la ejecucion del metodo iniciarRecepcionDeSincronizacionDeDatos.
   	                	this.secundario = false;
   	                }
   	                
   	                	
   				}
   			 }
   			 else
   				if(!this.secundario) {
   					this.sincronizacionDeDatos = new Thread(() -> iniciarRecepcionDeSincronizacionDeDatos(puertoSincronizacion));
   					this.sincronizacionDeDatos.start();
   					this.secundario = true;
   				}
   			
   			 socketEstado.close();
    		}
		}
    	
    	catch (Exception e) {
  		}
    }
    

	private void registrarseAlMonitor() {
    	
    	 Socket socket;
		try {
			socket = new Socket("localhost", 10009);
			 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
	         out.flush();
	         out.writeObject(this);
	         out.close();
	         socket.close();
		}
         
         catch (Exception e) {
 			e.printStackTrace();
 		}
	}
    
    

	private void iniciarServidorRegistros(int puerto) {
        try {
            serverSocketRegistros = new ServerSocket(puerto);
            log("Servidor de registros iniciado en el puerto " + puerto);
            while (true) {
                Socket socketRegistros = serverSocketRegistros.accept();
                ObjectInputStream in = new ObjectInputStream(socketRegistros.getInputStream());
                Usuario usuario = (Usuario) in.readObject();
                //mandar usuario registrado al otro/s servidor
                manejarRegistros(usuario);
            }
        } catch (Exception e) {
            log("Error al iniciar el servidor de registros, línea 49: " + e.getMessage());
        }
    }

    private void iniciarServidorMensajes(int puerto) {
        try {
            serverSocketMensajes = new ServerSocket(puerto);
            log("Servidor de Mensajes iniciado en el puerto " + puerto);
            while (true) {
                Socket socketMensajes = serverSocketMensajes.accept();
                ObjectInputStream in = new ObjectInputStream(socketMensajes.getInputStream());
                Mensaje mensaje = (Mensaje) in.readObject();
                manejarMensajes(mensaje);
            }
        } catch (IOException | ClassNotFoundException e) {
            log("Error al iniciar el servidor de mensajes, línea 64: " + e.getMessage());
        }
    }

    private void iniciarServidorDirectorio(int puerto) {
        try {
            serverSocketDirectorio = new ServerSocket(puerto);
            log("Servidor de Directorio iniciado en el puerto " + puerto);
            while (true) {
                Socket socketRecibePedido = serverSocketDirectorio.accept();
                ObjectOutputStream out = new ObjectOutputStream(socketRecibePedido.getOutputStream());
                out.flush();
                out.writeObject(this.directorioUsuarios);
                out.close();
            }
        } catch (IOException e) {
            log("Error al iniciar el servidor de directorio, línea 92: " + e.getMessage());
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
                    this.MapPendientes.get(mensaje.getNicknameDestinatario()).addLast(mensaje);
                    //mandar mensaje al otro/s servidor
                }
            } finally {
                // Cierra los recursos
                try {
                    if (out != null) {
                        out.close();
                    }
                    if (socket2 != null && !socket2.isClosed()) {
                        socket2.close();
                    }
                } catch (IOException e) {
                    log("Error al cerrar los recursos: " + e.getMessage());
                }
            }
        }).start();
    }

    private void manejarRegistros(Usuario usuario) {
        String nickname = usuario.getNickname();
        if (!directorioUsuarios.containsKey(nickname)) {
            directorioUsuarios.put(nickname, usuario);
            log("Usuario registrado y logueado: " + nickname + ", en el puerto " + usuario.getPuerto());
            this.MapPendientes.put(nickname, new LinkedList<Mensaje>());
        } else {
            log("Usuario Logueado: " + nickname + ", en el puerto " + usuario.getPuerto());
            enviarMensajesPendientes(nickname);
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
                    // Cierra los recursos
                    try {
                        if (out != null) {
                            out.close();
                        }
                        if (socket2 != null && !socket2.isClosed()) {
                            socket2.close();
                        }
                    } catch (IOException e) {
                        log("Error al cerrar los recursos: " + e.getMessage());
                    }
                }
                pendientes.removeFirst();
            }
        }
    }
    

    public int getPuertoEstado() {
		return puertoEstado;
	}

	public int getPuertoMensajes() {
		return puertoMensajes;
	}

	public int getPuertoRegistros() {
		return puertoRegistros;
	}

	public int getPuertoDirectorio() {
		return puertoDirectorio;
	}

	public ServerSocket getServerSocketRegistros() {
		return serverSocketRegistros;
	}

	public ServerSocket getServerSocketMensajes() {
		return serverSocketMensajes;
	}

	public ServerSocket getServerSocketDirectorio() {
		return serverSocketDirectorio;
	}

	private void log(String mensaje) {
        SwingUtilities.invokeLater(() -> logArea.append(mensaje + "\n"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Servidor());
    }
}
