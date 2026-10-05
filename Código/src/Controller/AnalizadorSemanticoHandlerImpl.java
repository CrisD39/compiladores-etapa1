package Controller;

import Model.AnalizadorLexico;
import Model.AnalizadorLexicoImpl;
import Model.AnalizadorSintactico;
import Model.AnalizadorSintacticoImpl;
import Model.ResultadoLexicoListener;
import Model.SourceManager;
import Model.SourceManagerMejorado;
import Model.TablaSimbolos;

import java.io.IOException;

public class AnalizadorSemanticoHandlerImpl implements AnalizadorSemanticoHandler {

    @Override
    public ResultadoAnalisisSemantico analizar(String rutaArchivo, ResultadoLexicoListener listener) throws IOException {
        SourceManager sourceManager = new SourceManagerMejorado();
        sourceManager.open(rutaArchivo);
        try {
            AnalizadorLexico lexico = new AnalizadorLexicoImpl(sourceManager, listener);
            TablaSimbolos tablaSimbolos = new TablaSimbolos();
            AnalizadorSintactico sintactico = new AnalizadorSintacticoImpl(lexico, tablaSimbolos);
            sintactico.start();
            // Chequeo de corrección (ver TablaSimbolos.consolidar()): corre
            // siempre, aun si hubo errores sintácticos -- opera sobre lo que
            // efectivamente se llegó a registrar en la tabla.
            tablaSimbolos.consolidar();
            return new ResultadoAnalisisSemantico(sintactico.getErrores(), tablaSimbolos.getErrores());
        } finally {
            try {
                sourceManager.close();
            } catch (IOException ignored) {
                // cerrar el fuente no debe tapar el resultado del análisis
            }
        }
    }
}
