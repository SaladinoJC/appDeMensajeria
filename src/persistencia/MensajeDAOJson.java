package persistencia;

import mensajeria.Mensaje;
import mensajeria.Contacto;

import java.io.*;
import java.util.*;

public class MensajeDAOJson implements MensajeDAO {
    private String archivoMensajes;
    private String archivoContactos;

    public MensajeDAOJson(String archivoBase) {
        this.archivoMensajes = archivoBase;
        this.archivoContactos = archivoBase.replace(".json", "_contactos.json");
    }

    // ----------- MENSAJES -----------
    @Override
    public void guardarMensajes(String chatId, List<Mensaje> mensajes) throws Exception {
        Map<String, List<Mensaje>> todos = cargarTodosLosChats();
        todos.put(chatId, mensajes);

        try (PrintWriter pw = new PrintWriter(new FileWriter(archivoMensajes))) {
            pw.print("{\n");
            boolean primerChat = true;
            for (String chat : todos.keySet()) {
                if (!primerChat) pw.print(",\n");
                pw.print("  \"" + escapeJson(chat) + "\": [");
                List<Mensaje> ms = todos.get(chat);
                for (int i = 0; i < ms.size(); i++) {
                    Mensaje m = ms.get(i);
                    pw.print("{");
                    pw.print("\"remitente\":\"" + escapeJson(m.getNicknameRemitente()) + "\",");
                    pw.print("\"puertoRemitente\":" + m.getPuertoRemitente() + ",");
                    pw.print("\"ipdestinatario\":\"" + escapeJson(m.getIpDestinatario()) + "\",");
                    pw.print("\"puertoDestinatario\":" + m.getPuertoDestinatario() + ",");
                    pw.print("\"nicknameDestinatario\":\"" + escapeJson(m.getNicknameDestinatario()) + "\",");
                    pw.print("\"contenido\":\"" + escapeJson(m.getContenido()) + "\",");
                    pw.print("\"timestamp\":" + m.getTimestamp().getTime());
                    pw.print("}");
                    if (i < ms.size() - 1) pw.print(",");
                }
                pw.print("]");
                primerChat = false;
            }
            pw.print("\n}\n");
        }
    }

    @Override
    public List<Mensaje> cargarMensajes(String chatId) throws Exception {
        Map<String, List<Mensaje>> todos = cargarTodosLosChats();
        return todos.getOrDefault(chatId, new ArrayList<>());
    }

    private Map<String, List<Mensaje>> cargarTodosLosChats() throws Exception {
        Map<String, List<Mensaje>> chats = new HashMap<>();
        File file = new File(archivoMensajes);
        if (!file.exists() || file.length() == 0) return chats;

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String l;
            while ((l = br.readLine()) != null) sb.append(l);
        }

        String json = sb.toString().trim();
        if (!json.startsWith("{") || !json.endsWith("}")) return chats;
        json = json.substring(1, json.length() - 1); // sin llaves exteriores

        int i = 0;
        while (i < json.length()) {
            // Leer clave (chatId)
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
            if (json.charAt(i) != '"') break;
            int inicioClave = ++i;
            while (i < json.length() && json.charAt(i) != '"') {
                if (json.charAt(i) == '\\') i++; // saltar carácter escapado
                i++;
            }
            String chatId = unescapeJson(json.substring(inicioClave, i++));
            while (i < json.length() && json.charAt(i) != '[') i++; // buscar '['
            i++; // avanzar después del [

            List<Mensaje> mensajes = new ArrayList<>();
            while (i < json.length() && json.charAt(i) != ']') {
                while (i < json.length() && json.charAt(i) != '{') i++;
                i++; // después del {
                Map<String, String> campos = new HashMap<>();
                StringBuilder clave = new StringBuilder();
                StringBuilder valor = new StringBuilder();
                boolean leyendoClave = true, enString = false, escapando = false;
                String actual = null;

                while (i < json.length()) {
                    char c = json.charAt(i);

                    if (leyendoClave) {
                        if (c == '"' && !escapando) {
                            enString = !enString;
                            if (!enString) actual = clave.toString(); // cerró clave
                        } else if (enString) {
                            if (c == '\\' && !escapando) escapando = true;
                            else {
                                clave.append(c);
                                escapando = false;
                            }
                        } else if (c == ':') {
                            leyendoClave = false;
                            enString = false;
                            clave = new StringBuilder();
                        }
                    } else {
                        if (c == '"' && !escapando) {
                            enString = !enString;
                            if (!enString) {
                                campos.put(actual, valor.toString());
                                valor = new StringBuilder();
                                actual = null;
                            }
                        } else if (!enString && (c == ',' || c == '}')) {
                            if (valor.length() > 0 && actual != null) {
                                campos.put(actual, valor.toString());
                                valor = new StringBuilder();
                                actual = null;
                            }
                            leyendoClave = true;
                            enString = false;
                            if (c == '}') break;
                        } else if (enString || (Character.isDigit(c) || c == '-' || c == '.')) {
                            if (c == '\\' && !escapando) escapando = true;
                            else {
                                valor.append(c);
                                escapando = false;
                            }
                        }
                    }
                    i++;
                }

                try {
                    Mensaje m = new Mensaje(
                        unescapeJson(campos.get("contenido")),
                        unescapeJson(campos.get("remitente")),
                        Integer.parseInt(campos.get("puertoRemitente")),
                        unescapeJson(campos.get("ipdestinatario")),
                        Integer.parseInt(campos.get("puertoDestinatario")),
                        unescapeJson(campos.get("nicknameDestinatario"))
                    );
                    m.setTimestamp(new Date(Long.parseLong(campos.get("timestamp"))));
                    mensajes.add(m);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                while (i < json.length() && json.charAt(i) != '{' && json.charAt(i) != ']') i++;
            }

            chats.put(chatId, mensajes);
            while (i < json.length() && json.charAt(i) != '"') i++; // avanzar al siguiente chatId
        }

        return chats;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private String unescapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    // ----------- CONTACTOS -----------
    @Override
    public void guardarContactos(List<Contacto> contactos) throws Exception {
        try (PrintWriter pw = new PrintWriter(new FileWriter(archivoContactos))) {
            pw.print("[");
            for (int i = 0; i < contactos.size(); i++) {
                Contacto c = contactos.get(i);
                pw.print("{");
                pw.print("\"nombre\":\"" + escapeJson(c.getNombre()) + "\",");
                pw.print("\"nickname\":\"" + escapeJson(c.getNickname()) + "\",");
                pw.print("\"direccionIP\":\"" + escapeJson(c.getDireccionIP()) + "\",");
                pw.print("\"puerto\":" + c.getPuerto());
                pw.print("}");
                if (i != contactos.size() - 1) pw.print(",");
            }
            pw.print("]");
        }
    }

    @Override
    public List<Contacto> cargarContactos() throws Exception {
        List<Contacto> lista = new ArrayList<>();
        File file = new File(archivoContactos);
        if (!file.exists()) return lista;
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String l;
            while ((l = br.readLine()) != null) sb.append(l);
        }

        String json = sb.toString().trim();
        if (!json.startsWith("[") || !json.endsWith("]")) return lista;

        String body = json.substring(1, json.length() - 1);
        String[] elementos = body.split("\\},\\{");

        for (String s : elementos) {
            s = s.replaceAll("^\\{", "").replaceAll("}$", "");
            String[] pares = s.split("\",\"");
            Map<String, String> datos = new HashMap<>();
            for (String p : pares) {
                String[] kv = p.split("\":\"");
                if (kv.length == 2)
                    datos.put(kv[0].replace("\"", ""), kv[1].replace("\"", ""));
                else if (kv.length == 1 && kv[0].contains(":")) {
                    String[] kv2 = kv[0].split(":");
                    datos.put(kv2[0].replace("\"", ""), kv2[1].replace("\"", ""));
                }
            }
            lista.add(new Contacto(
                unescapeJson(datos.get("nombre")),
                unescapeJson(datos.get("direccionIP")),
                Integer.parseInt(datos.get("puerto")),
                unescapeJson(datos.get("nickname"))
            ));
        }

        return lista;
    }
}
