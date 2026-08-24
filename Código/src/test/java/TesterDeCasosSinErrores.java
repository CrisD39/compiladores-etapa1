package test.java;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import View.ModuloPrincipal;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.MatcherAssert.assertThat;
@RunWith(Parameterized.class)
public class TesterDeCasosSinErrores {

    private static final String MSG_EXITO = "[SinErrores]";
    private static final String TEST_FILES_DIRECTORY_PATH = "resources/sinErrores/";
    private static final String EXPECTED_TOKEN_PREFIX = "//#";

    private static final ModuloPrincipal init = null;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    // Cambiar a true solo para ver la salida del compilador durante la ejecucion de los tests.
    private final boolean fullCompilerOutputPrintingInEachTest = false;

    private final String input;

    public TesterDeCasosSinErrores(String input) {
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
        List<TokenEsperado> tokensEsperados = leerTokensEsperados(path);

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

        List<TokenEsperado> tokensObtenidos = extraerTokensImpresos(salida, path);

        assertThat(
                "Cantidad incorrecta de tokens en: " + path
                        + "\nEsperados: " + tokensEsperados
                        + "\nObtenidos: " + tokensObtenidos
                        + "\nSalida completa:\n" + salida,
                tokensObtenidos.size(),
                CoreMatchers.is(tokensEsperados.size())
        );

        for (int i = 0; i < tokensEsperados.size(); i++) {
            TokenEsperado esperado = tokensEsperados.get(i);
            TokenEsperado obtenido = tokensObtenidos.get(i);

            assertThat(
                    "Lexema incorrecto en el token " + (i + 1) + " de " + path
                            + "\nEsperado: " + esperado
                            + "\nObtenido: " + obtenido
                            + "\nSalida completa:\n" + salida,
                    obtenido.lexema,
                    CoreMatchers.is(esperado.lexema)
            );

            assertThat(
                    "Numero de linea incorrecto en el token " + (i + 1) + " de " + path
                            + "\nEsperado: " + esperado
                            + "\nObtenido: " + obtenido
                            + "\nSalida completa:\n" + salida,
                    obtenido.linea,
                    CoreMatchers.is(esperado.linea)
            );
        }
    }

    /*
     * Cada expectativa se escribe en una linea de comentario, preferentemente
     * al final del archivo fuente, con el siguiente formato estricto:
     *
     * //#<lexema>,<numeroDeLinea>
     *
     * Ejemplo:
     *
     * class A {
     * }
     * //#class,1
     * //#A,1
     * //#{,1
     * //#},2
     *
     * El lexema es la secuencia textual exacta reconocida en el fuente. Por eso:
     *
     * "hola"    -> //#"hola",1
     * 'c'        -> //#'c',2
     * '\n'       -> //#'\n',3
     *
     * Se toma la ultima coma como separador. Asi tambien se admiten lexemas que
     * contienen comas, por ejemplo: //#"hola, mundo",1
     */
    private List<TokenEsperado> leerTokensEsperados(String path) {
        List<TokenEsperado> tokensEsperados = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            int sourceLineNumber = 0;

            while ((line = reader.readLine()) != null) {
                sourceLineNumber++;

                if (line.startsWith(EXPECTED_TOKEN_PREFIX)) {
                    tokensEsperados.add(
                            parsearTokenEsperado(
                                    line.substring(EXPECTED_TOKEN_PREFIX.length()),
                                    path,
                                    sourceLineNumber
                            )
                    );
                }
            }

        } catch (IOException exception) {
            throw new RuntimeException("No se pudo leer el caso de prueba: " + path, exception);
        }

        return tokensEsperados;
    }

    private TokenEsperado parsearTokenEsperado(
            String expectedTokenText,
            String path,
            int sourceLineNumber
    ) {
        int lastComma = expectedTokenText.lastIndexOf(',');

        if (lastComma < 0) {
            throw new RuntimeException(
                    "Expectativa invalida en " + path + ", linea " + sourceLineNumber
                            + ". Se esperaba el formato: //#<lexema>,<numeroDeLinea>"
            );
        }

        String lexema = expectedTokenText.substring(0, lastComma);
        String lineNumberText = expectedTokenText.substring(lastComma + 1).trim();

        try {
            return new TokenEsperado(lexema, Integer.parseInt(lineNumberText));
        } catch (NumberFormatException exception) {
            throw new RuntimeException(
                    "Numero de linea invalido en " + path + ", linea " + sourceLineNumber
                            + ": " + lineNumberText,
                    exception
            );
        }
    }

    /*
     * Se ignora el primer campo de la salida del alumno, porque no
     * fijamos los nombres internos de tokens. Solo se verifican lexema y linea.
     *
     * Se toma la primera y la ultima coma para admitir lexemas con comas:
     * (NombreToken,Lexema,NroLinea)
     */
    private List<TokenEsperado> extraerTokensImpresos(String salida, String path) {
        List<TokenEsperado> tokens = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new StringReader(salida))) {
            String line;
            int outputLineNumber = 0;

            while ((line = reader.readLine()) != null) {
                outputLineNumber++;
                String trimmedLine = line.trim();

                if (trimmedLine.startsWith("(") && trimmedLine.endsWith(")")) {
                    tokens.add(parsearTokenImpreso(trimmedLine, path, outputLineNumber));
                }
            }

        } catch (IOException exception) {
            // StringReader no produce IOException durante una lectura normal.
            throw new RuntimeException("No se pudo procesar la salida de: " + path, exception);
        }

        return tokens;
    }

    private TokenEsperado parsearTokenImpreso(
            String tokenImpreso,
            String path,
            int outputLineNumber
    ) {
        int firstComma = tokenImpreso.indexOf(',');
        int lastComma = tokenImpreso.lastIndexOf(',');

        if (firstComma <= 1 || lastComma <= firstComma || lastComma >= tokenImpreso.length() - 2) {
            throw new AssertionError(
                    "Formato de token invalido en la salida de " + path
                            + ", linea de salida " + outputLineNumber + ": " + tokenImpreso
                            + "\nSe esperaba: (NombreToken,Lexema,NroLinea)"
            );
        }

        String lexema = tokenImpreso.substring(firstComma + 1, lastComma);
        String lineNumberText = tokenImpreso
                .substring(lastComma + 1, tokenImpreso.length() - 1)
                .trim();

        try {
            return new TokenEsperado(lexema, Integer.parseInt(lineNumberText));
        } catch (NumberFormatException exception) {
            throw new AssertionError(
                    "El numero de linea no es valido en la salida de " + path
                            + ", linea de salida " + outputLineNumber + ": " + tokenImpreso
            );
        }
    }

    private static class TokenEsperado {
        private final String lexema;
        private final int linea;

        private TokenEsperado(String lexema, int linea) {
            this.lexema = lexema;
            this.linea = linea;
        }

        @Override
        public String toString() {
            return "(" + lexema + "," + linea + ")";
        }
    }
}
