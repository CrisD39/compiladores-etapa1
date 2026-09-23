package View;

import Controller.AnalizadorSintacticoHandler;
import Controller.AnalizadorSintacticoHandlerImpl;
import Model.ErrorLexico;
import Model.ErrorSintactico;
import Model.ResultadoLexicoListener;
import Model.Token;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

// Módulo Principal de la etapa 2: corre el analizador SINTÁCTICO sobre el fuente.
// Es el espejo de ModuloPrincipal (que es solo léxico); se mantiene aparte para
// no tocar la cadena de la etapa 1. El wiring (abrir archivo, armar léxico +
// sintáctico) vive en AnalizadorSintacticoHandlerImpl (Controller), análogo a
// AnalizadorHandlerImpl para el léxico: acá sólo queda la presentación.
public final class ModuloPrincipalET2 implements ResultadoLexicoListener {

    private boolean huboErroresLexicos = false;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Uso: java View.ModuloPrincipalET2 <archivo-fuente>");
            return;
        }
        new ModuloPrincipalET2().ejecutar(args[0]);
    }

    private void ejecutar(String rutaArchivo) {
        AnalizadorSintacticoHandler handler = new AnalizadorSintacticoHandlerImpl();
        try {
            List<ErrorSintactico> erroresSintacticos = handler.analizar(rutaArchivo, this);

            for (ErrorSintactico error : erroresSintacticos) {
                reportarError(error);
            }

            if (!huboErroresLexicos && erroresSintacticos.isEmpty()) {
                System.out.println("[SinErrores]");
            }
        } catch (IOException e) {
            System.out.println("No se pudo abrir el archivo fuente: " + rutaArchivo);
        } catch (UncheckedIOException e) {
            System.out.println("No se pudo leer el archivo fuente: " + rutaArchivo);
        }
    }

    // Formato análogo al error léxico de ModuloPrincipal: una línea legible y la
    // etiqueta [Error:<lexema>|<linea>] que consumen los testers.
    private void reportarError(ErrorSintactico error) {
        System.out.println("Error Sintáctico en línea " + error.getLinea()
                + ": se esperaba " + error.getEsperado()
                + " y se encontró " + error.getEncontrado() + ".");
        System.out.println("[Error:" + error.getLexema() + "|" + error.getLinea() + "]");
    }

    // En modo pull el sintáctico consume los tokens uno a uno; no se listan.
    @Override
    public void onToken(Token token) {
    }

    // El léxico sigue empujando sus errores por acá; se informan pero no cortan
    // el análisis sintáctico (el sintáctico solo ve tokens).
    @Override
    public void onError(ErrorLexico error) {
        huboErroresLexicos = true;
        System.out.println("Error Léxico en línea " + error.getLinea() + ": "
                + error.getLexema() + " " + error.getRazon());
        System.out.println("[Error:" + error.getLexema() + "|" + error.getLinea() + "]");
    }
}
