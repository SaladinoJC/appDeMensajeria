package appDeMensajeria;
import java.net.*;
import java.io.*;
public class Servidor {
    public static void main(String args[]) {
        try {
            ServerSocket listenSocket = new ServerSocket(1234);
            Socket clientSocket = listenSocket.accept();
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            
            String data = in.readUTF();
            out.writeUTF(data.toUpperCase());
        } catch (IOException e) {
            System.out.println("Listen :" + e.getMessage());
        }
    }
}