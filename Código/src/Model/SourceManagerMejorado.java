package Model;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

// Implementación alternativa de SourceManager: en vez de pedir un carácter a
// la vez a un BufferedReader (que en cada llamada a read() paga overhead de
// sincronización y chequeo de límites), lee directo del Reader en bloques de
// TAMANO_BUFFER caracteres con Reader.read(char[]) y reparte esos caracteres
// desde un arreglo propio. Arma la línea completa antes de empezar a repartir
// sus caracteres uno a uno, lo que de paso permite exponer getLineaActual()
// con la línea entera (para REQ-MP-08) y distinguir sin ambigüedad si la
// última línea del archivo terminó o no en un salto de línea real.
public class SourceManagerMejorado implements SourceManager {
    private static final int TAMANO_BUFFER = 8192;

    private Reader reader;
    private final char[] buffer = new char[TAMANO_BUFFER];
    private int bufferLength;
    private int bufferPos;

    private final StringBuilder currentLine = new StringBuilder();
    private boolean currentLineTerminada;
    private int lineIndexNumber;
    private int lineNumber;
    private boolean mustReadNextLine;
    private boolean eof;

    public SourceManagerMejorado() {
        bufferLength = 0;
        bufferPos = 0;
        currentLineTerminada = false;
        lineIndexNumber = 0;
        lineNumber = 0;
        mustReadNextLine = true;
        eof = false;
    }

    @Override
    public void open(String filePath) throws FileNotFoundException {
        FileInputStream fileInputStream = new FileInputStream(filePath);
        reader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    @Override
    public char getNextChar() throws IOException {
        if (eof) {
            return END_OF_FILE;
        }

        if (mustReadNextLine) {
            boolean lineaAnteriorTerminada = currentLineTerminada;
            boolean huboLinea = leerLinea();

            if (!huboLinea) {
                eof = true;
                // La línea anterior sí terminaba en un salto de línea real,
                // así que ese salto abre una línea más (vacía) donde cae el
                // EOF. Si la última línea del archivo no tenía salto final,
                // el EOF queda en esa misma línea, sin sumar una de más.
                if (lineaAnteriorTerminada) {
                    lineNumber++;
                }
                return END_OF_FILE;
            }

            lineNumber++;
            lineIndexNumber = 0;
            mustReadNextLine = false;
        }

        if (lineIndexNumber < currentLine.length()) {
            char actual = currentLine.charAt(lineIndexNumber);
            lineIndexNumber++;
            return actual;
        }

        mustReadNextLine = true;

        if (currentLineTerminada) {
            return '\n';
        }

        return getNextChar();
    }

    // Lee, carácter a carácter (desde el buffer propio), hasta encontrar un
    // salto de línea (\n, \r o \r\n, normalizados a uno solo) o el fin del
    // archivo, dejando el resultado en currentLine. Devuelve false solo si
    // no había ningún carácter más para leer (fin de archivo real).
    // currentLineTerminada indica si la línea leída terminó en un salto de
    // línea real, a diferencia de haber sido cortada por el EOF.
    private boolean leerLinea() throws IOException {
        int leido = leerCrudo();

        if (leido == -1) {
            // No hay línea nueva de verdad: currentLine se deja intacta, ya
            // que todavía puede hacer falta (ej. reportarError() mostrando
            // la línea de un error que corta justo contra el EOF).
            currentLineTerminada = false;
            return false;
        }

        currentLine.setLength(0);

        while (leido != -1 && leido != '\n' && leido != '\r') {
            currentLine.append((char) leido);
            leido = leerCrudo();
        }

        currentLineTerminada = (leido != -1);

        if (leido == '\r') {
            int siguiente = leerCrudo();
            if (siguiente != '\n' && siguiente != -1) {
                // No era un \r\n: hay que devolver ese carácter al buffer
                // para no perderlo. Como el carácter recién leído siempre
                // viene del buffer propio (no de un refill posterior),
                // retroceder el cursor alcanza para "reponerlo".
                bufferPos--;
            }
        }

        return true;
    }

    // Entrega el próximo carácter crudo del archivo, recargando el buffer en
    // bloques de TAMANO_BUFFER caracteres cuando se agota. Devuelve -1 en
    // fin de archivo real.
    private int leerCrudo() throws IOException {
        if (bufferPos >= bufferLength) {
            bufferLength = reader.read(buffer);
            bufferPos = 0;
            if (bufferLength <= 0) {
                return -1;
            }
        }
        return buffer[bufferPos++];
    }

    @Override
    public String getLineaActual() {
        return currentLine.toString();
    }

    @Override
    public int getLineNumber() {
        return lineNumber;
    }
}
