package Controller;

import Model.AnalizadorLexico;
import Model.AnalizadorLexicoImpl;
import Model.ResultadoLexicoListener;
import Model.SourceManager;
import Model.SourceManagerMejorado;

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
