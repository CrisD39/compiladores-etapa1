package test.java;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import View.ModuloPrincipalET2;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

// Gemelo "no parametrizado" de TesterSintacticoDeCasosConErrores: ese harness
// sólo puede verificar un código de error por archivo (lo declara en la primera
// línea), así que no puede probar el comportamiento realmente nuevo del modo
// pánico (REQ-AS-008): que aparezcan VARIOS errores independientes en una sola
// corrida. Recursos en resources/sintactico/panico/ (carpeta aparte, no la
// escanea ningún harness genérico). El timeout es un guardrail de regresión:
// si algún refactor futuro reintroduce un loop sin progreso en sincronizar(),
// el test falla rápido en vez de colgar la corrida.
public class TesterSintacticoModoPanico {

    private static final String DIR = "resources/sintactico/panico/";

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @Before
    public void setUp() {
        System.setOut(new PrintStream(outContent));
    }

    @After
    public void tearDown() {
        System.setOut(originalOut);
    }

    // Caso que expuso el bug de diseño durante la validación: "new Foo;" falla
    // justo cuando el token que sigue ya es un ";" de sincronización. Si
    // sincronizar() lo descartara de más, el error independiente de la
    // siguiente sentencia ("int y = ;") quedaría tragado.
    @Test(timeout = 5000)
    public void dosErroresAdyacentesSeReportanLosDos() {
        ModuloPrincipalET2.main(new String[]{ DIR + "panicoDosErroresAdyacentes.java" });
        String salida = capturar();
        assertThat(salida, allOf(
                containsString("[Error:;|8]"),
                containsString("[Error:int|9]")));
    }

    // Un método sin ";" de cierre (recupera cruzando el límite del "}") y otro
    // con un token que no puede arrancar ninguna sentencia ("%"). El "%;" sin
    // consumir deja el resto del archivo desalineado un nivel (una cascada real,
    // no simulada): el análisis sigue igual, sin colgarse, hasta el EOF.
    @Test(timeout = 5000)
    public void dosErroresEnMetodosDistintosSeReportanEIndicanElArrastre() {
        ModuloPrincipalET2.main(new String[]{ DIR + "panicoDosErroresEnMetodosDistintos.java" });
        String salida = capturar();
        assertThat(salida, allOf(
                containsString("[Error:}|9]"),
                containsString("[Error:%|12]"),
                containsString("[Error:}|16]")));
    }

    // Caso límite: no hay ";"/"{"/"}" entre el error y el fin del archivo.
    // sincronizar() tiene que terminar en EOF (el léxico lo repite para
    // siempre) sin colgarse -- de ahí el timeout como red de seguridad real.
    @Test(timeout = 5000)
    public void sinTokenDeSincronizacionHastaEofNoCicla() {
        ModuloPrincipalET2.main(new String[]{ DIR + "panicoSinSincronizacionHastaEof.java" });
        String salida = capturar();
        assertThat(salida, containsString("[Error:$|8]"));
        assertThat(salida, not(containsString("[SinErrores]")));
    }

    private String capturar() {
        System.setOut(originalOut);
        String salida = outContent.toString();
        outContent.reset();
        return salida;
    }
}
