package Controller;

import Model.AnalizadorLexico;
import Model.AnalizadorLexicoImpl;
import Model.AnalizadorSintactico;
import Model.AnalizadorSintacticoImpl;
import Model.ErrorSintactico;
import Model.ResultadoLexicoListener;
import Model.SourceManager;
import Model.SourceManagerMejorado;

import java.io.IOException;
import java.util.List;

public class AnalizadorSintacticoHandlerImpl implements AnalizadorSintacticoHandler {

    @Override
    public List<ErrorSintactico> analizar(String rutaArchivo, ResultadoLexicoListener listener) throws IOException {
        SourceManager sourceManager = new SourceManagerMejorado();
        sourceManager.open(rutaArchivo);
        try {
            AnalizadorLexico lexico = new AnalizadorLexicoImpl(sourceManager, listener);
            AnalizadorSintactico sintactico = new AnalizadorSintacticoImpl(lexico);
            sintactico.start();
            return sintactico.getErrores();
        } finally {
            try {
                sourceManager.close();
            } catch (IOException ignored) {
                // cerrar el fuente no debe tapar el resultado del análisis
            }
        }
    }
}
