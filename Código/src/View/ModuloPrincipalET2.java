package View;

import Model.AnalizadorLexico;
import Model.AnalizadorLexicoImpl;
import Model.AnalizadorSintactico;
import Model.AnalizadorSintacticoImpl;
import Model.ErrorLexico;
import Model.ErrorSintactico;
import Model.ResultadoLexicoListener;
import Model.SourceManager;
import Model.SourceManagerMejorado;
import Model.Token;

import java.io.IOException;
import java.io.UncheckedIOException;

// Módulo Principal de la etapa 2: corre el analizador SINTÁCTICO sobre el fuente.
// Es el espejo de ModuloPrincipal (que es solo léxico); se mantiene aparte para
// no tocar la cadena de la etapa 1. El léxico se reusa en modo pull (nextToken()).
//
// Pendientes (ver Documentación/analizador_sintactico.md):
//  - Recuperación en modo pánico (REQ-AS-008): hoy corta y reporta el primer
//    error sintáctico.
//  - Mecanismo de reporte tipo listener y paso por AnalizadorHandler; acá el
//    wiring (abrir archivo, armar léxico + sintáctico) se hace directo.
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
        SourceManager sourceManager = new SourceManagerMejorado();
        try {
            sourceManager.open(rutaArchivo);
        } catch (IOException e) {
            System.out.println("No se pudo abrir el archivo fuente: " + rutaArchivo);
            return;
        }

        try {
            AnalizadorLexico lexico = new AnalizadorLexicoImpl(sourceManager, this);
            AnalizadorSintactico sintactico = new AnalizadorSintacticoImpl(lexico);

            sintactico.start();

            if (!huboErroresLexicos) {
                System.out.println("[SinErrores]");
            }
        } catch (ErrorSintactico e) {
            reportarError(e);
        } catch (UncheckedIOException e) {
            System.out.println("No se pudo leer el archivo fuente: " + rutaArchivo);
        } finally {
            try {
                sourceManager.close();
            } catch (IOException ignored) {
                // cerrar el fuente no debe tapar el resultado del análisis
            }
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
