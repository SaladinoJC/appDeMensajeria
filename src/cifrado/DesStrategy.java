package cifrado;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Arrays;

public class DesStrategy implements CifradoStrategy {
    private final SecretKey key;

    public DesStrategy(String claveUsuario) throws Exception {
        // DES usa 8 bytes (64 bits)
        byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(claveUsuario.getBytes(StandardCharsets.UTF_8));
        byte[] keyBytes = Arrays.copyOf(hash, 8);
        this.key = new SecretKeySpec(keyBytes, "DES");
    }

    @Override
    public String cifrar(String textoPlano) throws Exception {
        Cipher cipher = Cipher.getInstance("DES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    @Override
    public String descifrar(String textoCifrado) throws Exception {
        Cipher cipher = Cipher.getInstance("DES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decoded = Base64.getDecoder().decode(textoCifrado);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}