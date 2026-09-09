package test.java;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import View.ModuloPrincipalET2;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.MatcherAssert.assertThat;

// Gemelo sintactico de TesterDeCasosSinErrores (que es lexico): misma mecanica
// (parametrizado sobre los archivos de una carpeta, captura de System.out,
// bandera de impresion), pero sobre casos del analizador sintactico y contra su
// propia carpeta de recursos. No compara token por token: para el sintactico
// alcanza con verificar que la corrida termine informando [SinErrores].
@RunWith(Parameterized.class)
public class TesterSintacticoDeCasosSinErrores {

    private static final String MSG_EXITO = "[SinErrores]";
    private static final String TEST_FILES_DIRECTORY_PATH = "resources/sintactico/sinErrores/";

    private static final ModuloPrincipalET2 init = null;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    // Cambiar a true solo para ver la salida del compilador durante la ejecucion de los tests.
    private final boolean fullCompilerOutputPrintingInEachTest = true;

    private final String input;

    public TesterSintacticoDeCasosSinErrores(String input) {
        this.input = input;
    }

    @Before
    public void setUp() {
        outContent.reset();
        System.setOut(new PrintStream(outContent));
    }

    @After
    public void tearDown() {
        System.setOut(originalOut);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<String> data() {
        File folder = new File(TEST_FILES_DIRECTORY_PATH);
        File[] files = folder.listFiles();

        if (files == null) {
            throw new RuntimeException(
                    "No se pudo leer el directorio de casos de prueba: "
                            + TEST_FILES_DIRECTORY_PATH
            );
        }

        List<String> names = new ArrayList<>();

        for (File file : files) {
            if (file.isFile()) {
                names.add(file.getName());
            }
        }

        Collections.sort(names);
        return names;
    }

    @Test
    public void testIterado() {
        probarExito(input);
    }

    private void probarExito(String name) {
        String path = TEST_FILES_DIRECTORY_PATH + name;

        init.main(new String[] { path });

        String salida = outContent.toString();

        if (fullCompilerOutputPrintingInEachTest) {
            originalOut.println("----- Salida de " + path + " -----");
            originalOut.print(salida);
            originalOut.println("--------------------------------");
        }

        assertThat(
                "No se informo analisis exitoso en: " + path
                        + "\nSalida obtenida:\n" + salida,
                salida,
                CoreMatchers.containsString(MSG_EXITO)
        );
    }
}
