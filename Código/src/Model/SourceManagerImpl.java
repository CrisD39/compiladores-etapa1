package Model;
//Author: Juan Dingevan

import java.io.*;
import java.nio.charset.StandardCharsets;

public class SourceManagerImpl implements SourceManager{
    private BufferedReader reader;
    private String currentLine;
    private boolean currentLineTerminada;
    private int lineNumber;
    private int lineIndexNumber;
    private boolean mustReadNextLine;
    private boolean eof;


    public SourceManagerImpl() {
        currentLine = "";
        currentLineTerminada = false;
        lineNumber = 0;
        lineIndexNumber = 0;
        mustReadNextLine = true;
        eof = false;
    }

    @Override
    public void open(String filePath) throws FileNotFoundException {
        FileInputStream fileInputStream = new FileInputStream(filePath);
        InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);

        reader = new BufferedReader(inputStreamReader);
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
            StringBuilder linea = new StringBuilder();
            boolean huboLinea = leerLinea(linea);

            if (!huboLinea) {
                eof = true;
                // La linea anterior sí terminaba en un salto de línea real, así
                // que ese salto abre una línea más (vacía) donde cae el EOF.
                // Si la última línea del archivo no tenía salto final, el EOF
                // queda en esa misma línea, sin sumar una de más.
                if (lineaAnteriorTerminada) {
                    lineNumber++;
                }
                return END_OF_FILE;
            }

            currentLine = linea.toString();
            lineNumber++;
            lineIndexNumber = 0;
            mustReadNextLine = false;
        }

        if (lineIndexNumber < currentLine.length()) {
            char currentChar = currentLine.charAt(lineIndexNumber);
            lineIndexNumber++;
            return currentChar;
        }

        mustReadNextLine = true;

        if (currentLineTerminada) {
            return '\n';
        }

        return getNextChar();
    }

    // Lee caracteres crudos hasta encontrar un salto de línea (\n, \r o \r\n,
    // que se normalizan a un solo carácter) o el fin del archivo. Devuelve
    // false solo si no había ningún carácter más para leer (fin de archivo
    // real). currentLineTerminada indica si la línea leída terminó en un
    // salto de línea real, a diferencia de haber sido cortada por el EOF.
    private boolean leerLinea(StringBuilder destino) throws IOException {
        int leido = reader.read();

        if (leido == -1) {
            currentLineTerminada = false;
            return false;
        }

        while (leido != -1 && leido != '\n' && leido != '\r') {
            destino.append((char) leido);
            leido = reader.read();
        }

        currentLineTerminada = (leido != -1);

        if (leido == '\r') {
            reader.mark(1);
            if (reader.read() != '\n') {
                reader.reset();
            }
        }

        return true;
    }

    @Override
    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public String getLineaActual() {
        return currentLine;
    }

}
