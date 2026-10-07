package Controller;

import Model.lexico.AnalizadorLexico;
import Model.lexico.AnalizadorLexicoImpl;
import Model.sintactico.AnalizadorSintactico;
import Model.sintactico.AnalizadorSintacticoImpl;
import Model.sintactico.ErrorSintactico;
import Model.lexico.ResultadoLexicoListener;
import Model.lexico.SourceManager;
import Model.lexico.SourceManagerMejorado;
import Model.semantico.TablaSimbolos;

import java.io.IOException;
import java.util.List;

public class AnalizadorSintacticoHandlerImpl implements AnalizadorSintacticoHandler {

    @Override
    public List<ErrorSintactico> analizar(String rutaArchivo, ResultadoLexicoListener listener) throws IOException {
        SourceManager sourceManager = new SourceManagerMejorado();
        sourceManager.open(rutaArchivo);
        try {
            AnalizadorLexico lexico = new AnalizadorLexicoImpl(sourceManager, listener);
            TablaSimbolos tablaSimbolos = new TablaSimbolos();
            AnalizadorSintactico sintactico = new AnalizadorSintacticoImpl(lexico, tablaSimbolos);
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
