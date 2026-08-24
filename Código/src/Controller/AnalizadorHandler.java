package Controller;

import Model.ResultadoLexicoListener;

import java.io.IOException;

// Puente entre el Módulo Principal (view), el manejador de archivos y el analizador
// léxico (REQ-AH-01). Solo se invoca a nivel grueso: una vez para armar y arrancar el
// análisis. El paso de tokens carácter a carácter no pasa por acá.
public interface AnalizadorHandler {

    void analizar(String rutaArchivo, ResultadoLexicoListener listener) throws IOException;
}
