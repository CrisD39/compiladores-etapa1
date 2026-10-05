package View;

import Controller.AnalizadorSemanticoHandler;
import Controller.AnalizadorSemanticoHandlerImpl;
import Controller.ResultadoAnalisisSemantico;
import Model.ErrorLexico;
import Model.ErrorSemantico;
import Model.ErrorSintactico;
import Model.ResultadoLexicoListener;
import Model.Token;

import java.io.IOException;
import java.io.UncheckedIOException;

// Módulo Principal de la etapa 3: corre léxico + sintáctico + el chequeo
// SEMÁNTICO de declaraciones (TablaSimbolos.consolidar()) sobre el fuente.
// Es el espejo de ModuloPrincipalET2 (que se queda en lo sintáctico); el
// wiring vive en AnalizadorSemanticoHandlerImpl (Controller), acá sólo queda
// la presentación.
public final class ModuloPrincipalET3 implements ResultadoLexicoListener {

    private boolean huboErroresLexicos = false;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Uso: java View.ModuloPrincipalET3 <archivo-fuente>");
            return;
        }
        new ModuloPrincipalET3().ejecutar(args[0]);
    }

    private void ejecutar(String rutaArchivo) {
        AnalizadorSemanticoHandler handler = new AnalizadorSemanticoHandlerImpl();
        try {
            ResultadoAnalisisSemantico resultado = handler.analizar(rutaArchivo, this);

            for (ErrorSintactico error : resultado.getErroresSintacticos()) {
                reportarErrorSintactico(error);
            }
            for (ErrorSemantico error : resultado.getErroresSemanticos()) {
                reportarErrorSemantico(error);
            }

            if (!huboErroresLexicos
                    && resultado.getErroresSintacticos().isEmpty()
                    && resultado.getErroresSemanticos().isEmpty()) {
                System.out.println("[SinErrores]");
            }
        } catch (IOException e) {
            System.out.println("No se pudo abrir el archivo fuente: " + rutaArchivo);
        } catch (UncheckedIOException e) {
            System.out.println("No se pudo leer el archivo fuente: " + rutaArchivo);
        }
    }

    // Formato análogo al error sintáctico de ModuloPrincipalET2: una línea
    // legible y la etiqueta [Error:<lexema>|<linea>] que consumen los testers.
    private void reportarErrorSintactico(ErrorSintactico error) {
        System.out.println("Error Sintáctico en línea " + error.getLinea()
                + ": se esperaba " + error.getEsperado()
                + " y se encontró " + error.getEncontrado() + ".");
        System.out.println("[Error:" + error.getLexema() + "|" + error.getLinea() + "]");
    }

    // Formato análogo, pero con la etiqueta [ErrorSem:<codigo>|<linea>] que
    // fija propuesta_casos_test_semantico.md (código por categoría, no
    // substring de mensaje libre).
    private void reportarErrorSemantico(ErrorSemantico error) {
        System.out.println("Error Semántico en línea " + error.getLinea()
                + ": " + error.getCodigo() + " (\"" + error.getLexema() + "\").");
        System.out.println("[ErrorSem:" + error.getCodigo() + "|" + error.getLinea() + "]");
    }

    // En modo pull el sintáctico consume los tokens uno a uno; no se listan.
    @Override
    public void onToken(Token token) {
    }

    // El léxico sigue empujando sus errores por acá; se informan pero no cortan
    // el análisis (igual que en ModuloPrincipalET2).
    @Override
    public void onError(ErrorLexico error) {
        huboErroresLexicos = true;
        System.out.println("Error Léxico en línea " + error.getLinea() + ": "
                + error.getLexema() + " " + error.getRazon());
        System.out.println("[Error:" + error.getLexema() + "|" + error.getLinea() + "]");
    }
}
