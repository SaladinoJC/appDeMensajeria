package cifrado;

public interface CifradoStrategy {
    String cifrar(String textoPlano) throws Exception;
    String descifrar(String textoCifrado) throws Exception;
}