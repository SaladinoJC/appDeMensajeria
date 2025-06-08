package cifrado;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

public final class KeyUtils {

    private KeyUtils() {}

    /**
     * Devuelve una clave AES-128 derivada SIEMPRE IGUAL para el par de usuarios,
     * sin importar el orden en que se pasen.
     */
    public static SecretKey claveCompartida(String nickA, String nickB) throws Exception {
        // 1) Orden alfabético para que A+B == B+A
        String claveTexto = (nickA.compareToIgnoreCase(nickB) <= 0)
                            ? nickA + nickB
                            : nickB + nickA;

        // 2) Hash y truncamos a 128 bits
        byte[] hash = MessageDigest.getInstance("SHA-256")
                                   .digest(claveTexto.getBytes(StandardCharsets.UTF_8));
        byte[] keyBytes = Arrays.copyOf(hash, 16);          // 128-bit
        return new SecretKeySpec(keyBytes, "AES");
    }
}
