package Model;

// Reporte de un error sintáctico (análogo a ErrorLexico, REQ-AS-003).
// Por ahora es una excepción con los datos mínimos: línea, qué se encontró y qué
// se esperaba. La versión "clase de datos + listener" (como ResultadoLexicoListener
// para el léxico) y la recuperación en modo pánico (REQ-AS-008) quedan pendientes:
// hoy el primer error corta el análisis.
public class ErrorSintactico extends RuntimeException {

    private final int linea;
    private final String encontrado;
    private final String esperado;

    public ErrorSintactico(int linea, String encontrado, String esperado) {
        super("Error sintáctico en línea " + linea + ": se esperaba " + esperado
                + " y se encontró " + encontrado + ".");
        this.linea = linea;
        this.encontrado = encontrado;
        this.esperado = esperado;
    }

    public int getLinea() {
        return linea;
    }

    public String getEncontrado() {
        return encontrado;
    }

    public String getEsperado() {
        return esperado;
    }
}
