package persistencia;

import mensajeria.Mensaje;
import mensajeria.Contacto;
import java.util.*;
import java.io.*;

public class MensajeDAOTxt implements MensajeDAO {
    private String archivoMensajes;
    private String archivoContactos;

    public MensajeDAOTxt(String archivoBase) {
        this.archivoMensajes = archivoBase;
        this.archivoContactos = archivoBase.replace(".txt", "_contactos.txt");
    }

    // ------------ MENSAJES ------------
    @Override
    public void guardarMensajes(String chatId, List<Mensaje> mensajes) throws Exception {
        Map<String, List<Mensaje>> todos = cargarTodosLosChats();
        todos.put(chatId, mensajes);

        try (PrintWriter pw = new PrintWriter(new FileWriter(archivoMensajes))) {
            for (String chat : todos.keySet()) {
                for (Mensaje m : todos.get(chat)) {
                    pw.println(chat + "|" +
                            m.getNicknameRemitente() + "|" +
                            m.getPuertoRemitente() + "|" +
                            m.getIpDestinatario() + "|" +
                            m.getPuertoDestinatario() + "|" +
                            m.getNicknameDestinatario() + "|" +
                            m.getContenido().replace("\n", "\\n") + "|" +
                            m.getTimestamp().getTime());
                }
            }
        }
    }

    @Override
    public List<Mensaje> cargarMensajes(String chatId) throws Exception {
        Map<String, List<Mensaje>> todos = cargarTodosLosChats();
        List<Mensaje> lista = todos.get(chatId);
        return lista != null ? lista : new ArrayList<>();
    }

    private Map<String, List<Mensaje>> cargarTodosLosChats() throws Exception {
        Map<String, List<Mensaje>> chats = new HashMap<>();
        File file = new File(archivoMensajes);
        if (!file.exists() || file.length() == 0) return chats;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String l;
            while ((l = br.readLine()) != null) {
                String[] p = l.split("\\|", 8);
                if (p.length == 8) {
                    Mensaje m = new Mensaje(
                            p[6].replace("\\n", "\n"),
                            p[1],
                            Integer.parseInt(p[2]),
                            p[3],
                            Integer.parseInt(p[4]),
                            p[5]
                    );
                    m.setTimestamp(new Date(Long.parseLong(p[7])));
                    String chat = p[0];
                    if (!chats.containsKey(chat))
                        chats.put(chat, new ArrayList<>());
                    chats.get(chat).add(m);
                }
            }
        }
        return chats;
    }

    // ------------ CONTACTOS ------------
    @Override
    public void guardarContactos(List<Contacto> contactos) throws Exception {
        try (PrintWriter pw = new PrintWriter(new FileWriter(archivoContactos))) {
            for (Contacto c : contactos) {
                pw.println(
                    c.getNombre() + "|" +
                    c.getNickname() + "|" +
                    c.getDireccionIP() + "|" +
                    c.getPuerto());
            }
        }
    }

    @Override
    public List<Contacto> cargarContactos() throws Exception {
        List<Contacto> lista = new ArrayList<>();
        File file = new File(archivoContactos);
        if (!file.exists()) return lista;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String l;
            while ((l = br.readLine()) != null) {
                String[] p = l.split("\\|");
                if (p.length == 4) {
                    lista.add(new Contacto(p[0], p[2], Integer.parseInt(p[3]), p[1]));
                }
            }
        }
        return lista;
    }
}