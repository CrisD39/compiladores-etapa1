package Controller;

import Model.lexico.AnalizadorLexico;
import Model.lexico.AnalizadorLexicoImpl;
import Model.lexico.ResultadoLexicoListener;
import Model.lexico.SourceManager;
import Model.lexico.SourceManagerMejorado;

import java.io.IOException;

public class AnalizadorHandlerImpl implements AnalizadorHandler {

    @Override
    public void analizar(String rutaArchivo, ResultadoLexicoListener listener) throws IOException {
        SourceManager sourceManager = new SourceManagerMejorado();
        sourceManager.open(rutaArchivo);
        try {
            AnalizadorLexico lexico = new AnalizadorLexicoImpl(sourceManager, listener);
            lexico.startAnalizar();
        } finally {
            sourceManager.close();
        }
    }
}
