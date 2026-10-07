package Controller;

import Model.lexico.ResultadoLexicoListener;

import java.io.IOException;

// Puente entre el Módulo Principal (view) y el chequeo semántico, análogo a
// AnalizadorSintacticoHandler. Corre léxico + sintáctico + consolidación
// semántica en una sola pasada y devuelve ambas listas de error.
public interface AnalizadorSemanticoHandler {

    ResultadoAnalisisSemantico analizar(String rutaArchivo, ResultadoLexicoListener listener) throws IOException;
}
