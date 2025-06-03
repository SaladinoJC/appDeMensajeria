package persistencia;

import mensajeria.Mensaje;
import mensajeria.Contacto;
import java.util.*;
import java.io.*;

public class MensajeDAOXml implements MensajeDAO {
    private String archivoMensajes;
    private String archivoContactos;

    public MensajeDAOXml(String archivoBase) {
        this.archivoMensajes = archivoBase;
        this.archivoContactos = archivoBase.replace(".xml", "_contactos.xml");
    }

    // ----------- MENSAJES -----------
    @Override
    public void guardarMensajes(String chatId, List<Mensaje> mensajes) throws Exception {
        Map<String, List<Mensaje>> todos = cargarTodosLosChats();
        todos.put(chatId, mensajes);

        try (PrintWriter pw = new PrintWriter(new FileWriter(archivoMensajes))) {
            pw.println("<mensajes>");
            for (String chat : todos.keySet()) {
                pw.println("\t<chat id=\"" + escapeXML(chat) + "\">");
                for (Mensaje m : todos.get(chat)) {
                    pw.print("\t\t<mensaje ");
                    pw.print("remitente=\"" + escapeXML(m.getNicknameRemitente()) + "\" ");
                    pw.print("puertoRemitente=\"" + m.getPuertoRemitente() + "\" ");
                    pw.print("ipdestinatario=\"" + escapeXML(m.getIpDestinatario()) + "\" ");
                    pw.print("puertoDestinatario=\"" + m.getPuertoDestinatario() + "\" ");
                    pw.print("nicknameDestinatario=\"" + escapeXML(m.getNicknameDestinatario()) + "\" ");
                    pw.print("contenido=\"" + escapeXML(m.getContenido()) + "\" ");
                    pw.print("timestamp=\"" + m.getTimestamp().getTime() + "\" ");
                    pw.println("/>");
                }
                pw.println("\t</chat>");
            }
            pw.println("</mensajes>");
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
            String line;
            String chatActual = null;
            List<Mensaje> mensajesActual = null;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("<chat id=\"")) {
                    int start = line.indexOf('"')+1;
                    int end = line.indexOf('"', start);
                    chatActual = unescapeXML(line.substring(start, end));
                    mensajesActual = new ArrayList<>();
                } else if (line.startsWith("</chat>")) {
                    if (chatActual != null && mensajesActual != null) {
                        chats.put(chatActual, mensajesActual);
                    }
                    chatActual = null;
                    mensajesActual = null;
                } else if (line.startsWith("<mensaje ") && chatActual != null) {
                    Map<String,String> attrs = extraerAtributosXML(line);
                    Mensaje m = new Mensaje(
                        attrs.get("contenido"),
                        attrs.get("remitente"),
                        Integer.parseInt(attrs.get("puertoRemitente")),
                        attrs.get("ipdestinatario"),
                        Integer.parseInt(attrs.get("puertoDestinatario")),
                        attrs.get("nicknameDestinatario")
                    );
                    m.setTimestamp(new Date(Long.parseLong(attrs.get("timestamp"))));
                    mensajesActual.add(m);
                }
            }
        }
        return chats;
    }

    private String escapeXML(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\"","&quot;").replace("'","&apos;");
    }
    private String unescapeXML(String s) {
        if (s==null) return "";
        return s.replace("&lt;","<").replace("&gt;",">").replace("&quot;","\"")
                .replace("&apos;","'").replace("&amp;","&");
    }
    private Map<String,String> extraerAtributosXML(String linea) {
        Map<String,String> map = new HashMap<>();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\w+)=\"([^\"]*)\"").matcher(linea);
        while (matcher.find()) {
            String nombre = matcher.group(1);
            String valor = unescapeXML(matcher.group(2));
            map.put(nombre, valor);
        }
        return map;
    }

    // ---------- CONTACTOS -----------
    @Override
    public void guardarContactos(List<Contacto> contactos) throws Exception {
        try (PrintWriter pw = new PrintWriter(new FileWriter(archivoContactos))) {
            pw.println("<contactos>");
            for (Contacto c : contactos) {
                pw.print("\t<contacto ");
                pw.print("nombre=\"" + escapeXML(c.getNombre()) + "\" ");
                pw.print("nickname=\"" + escapeXML(c.getNickname()) + "\" ");
                pw.print("direccionIP=\"" + escapeXML(c.getDireccionIP()) + "\" ");
                pw.print("puerto=\"" + c.getPuerto() + "\" ");
                pw.println("/>");
            }
            pw.println("</contactos>");
        }
    }

    @Override
    public List<Contacto> cargarContactos() throws Exception {
        List<Contacto> lista = new ArrayList<>();
        File file = new File(archivoContactos);
        if (!file.exists() || file.length() == 0) return lista;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("<contacto ")) {
                    Map<String,String> attrs = extraerAtributosXML(line);
                    lista.add(new Contacto(
                        attrs.get("nombre"),
                        attrs.get("direccionIP"),
                        Integer.parseInt(attrs.get("puerto")),
                        attrs.get("nickname")
                    ));
                }
            }
        }
        return lista;
    }
}