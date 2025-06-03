package persistencia;

public abstract class MensajeDAOFactory {
    public static final int XML = 1;
    public static final int JSON = 2;
    public static final int TXT = 3;

    // MÉTODO ABSTRACTO
    public abstract MensajeDAO crearMensajeDAO(String archivo);

    // FACTORY STATIC
    public static MensajeDAOFactory getFactory(int tipo) {
        switch (tipo) {
            case XML:  return new XMLMensajeDAOFactory();
            case JSON: return new JSONMensajeDAOFactory();
            case TXT:  return new TxtMensajeDAOFactory();
            default:   throw new IllegalArgumentException("Tipo de almacenamiento no válido");
        }
    }
}

// FACTORÍAS CONCRETAS
class XMLMensajeDAOFactory extends MensajeDAOFactory {
    @Override
    public MensajeDAO crearMensajeDAO(String archivo) {
        return new MensajeDAOXml(archivo);
    }
}
class JSONMensajeDAOFactory extends MensajeDAOFactory {
    @Override
    public MensajeDAO crearMensajeDAO(String archivo) {
        return new MensajeDAOJson(archivo);
    }
}
class TxtMensajeDAOFactory extends MensajeDAOFactory {
    @Override
    public MensajeDAO crearMensajeDAO(String archivo) {
        return new MensajeDAOTxt(archivo);
    }
}