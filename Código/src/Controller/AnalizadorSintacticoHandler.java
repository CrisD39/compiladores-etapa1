package Controller;

import Model.ErrorSintactico;
import Model.ResultadoLexicoListener;

import java.io.IOException;
import java.util.List;

// Puente entre el Módulo Principal (view) y el analizador sintáctico, análogo a
// AnalizadorHandler para el léxico. El listener sigue recibiendo los errores
// léxicos "de a uno" (el sintáctico consume el léxico en modo pull, pero los
// errores léxicos no cortan la secuencia de tokens); los errores sintácticos,
// en cambio, se acumulan durante el análisis (REQ-AS-008, modo pánico) y se
// devuelven recién al terminar, no vía listener.
public interface AnalizadorSintacticoHandler {

    List<ErrorSintactico> analizar(String rutaArchivo, ResultadoLexicoListener listener) throws IOException;
}
