package Model;
//Author: Juan Dingevan

import java.io.FileNotFoundException;
import java.io.IOException;

public interface SourceManager {
    void open(String filePath) throws FileNotFoundException;

    void close() throws IOException;

    char getNextChar() throws IOException;

    int getLineNumber();

    // Texto de la línea fuente donde está parado el análisis actualmente
    // (sin el salto de línea), para poder mostrarla completa en los
    // mensajes de error (REQ-MP-08).
    String getLineaActual();

    public static final char END_OF_FILE = (char) 26;
}
