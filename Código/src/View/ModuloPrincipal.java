package View;

import Controller.AnalizadorHandler;
import Controller.AnalizadorHandlerImpl;
import Model.ErrorLexico;
import Model.ResultadoLexicoListener;
import Model.Token;

import java.io.IOException;

// Módulo Principal (REQ-MP-*): interfaz CLI, toda la salida por System.out.
public final class ModuloPrincipal implements ResultadoLexicoListener {

    private boolean huboErrores = false;

    public static void main(String[] args) {
        if (args.length != 1) {
           //TODO: REVISAR
            //System.out.println("Uso: java -jar Compilador.jar <archivo-fuente>");
            return;
        }
        new ModuloPrincipal().ejecutar(args[0]);
    }

    private void ejecutar(String rutaArchivo) {
        AnalizadorHandler handler = new AnalizadorHandlerImpl();
        try {
            handler.analizar(rutaArchivo, this);
        } catch (IOException e) {
            System.out.println("No se pudo abrir el archivo fuente: " + rutaArchivo);
            return;
        }

        if (!huboErrores) {
            System.out.println("[SinErrores]");
        }
    }

    @Override
    public void onToken(Token token) {
        System.out.println(token);
    }

    @Override
    public void onError(ErrorLexico error) {
        huboErrores = true;
        System.out.println("Error Léxico en línea " + error.getLinea() + ": " + error.getRazon());
        System.out.println("Detalle: " + error.getLineaFuente());
        System.out.println(" ".repeat("Detalle: ".length() + error.getColumna()) + "^");
        System.out.println("[Error:" + error.getLexema() + "|" + error.getLinea() + "]");
    }
}
