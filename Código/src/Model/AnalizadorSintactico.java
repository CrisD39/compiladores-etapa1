package Model;

import java.util.List;

public interface AnalizadorSintactico {
    public void start();

    /** Errores sintácticos encontrados durante start() (REQ-AS-008, modo pánico). */
    public List<ErrorSintactico> getErrores();
}
