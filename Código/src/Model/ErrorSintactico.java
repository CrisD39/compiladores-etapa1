package Model;

// Reporte de un error sintáctico (análogo a ErrorLexico, REQ-AS-003). Clase de
// datos final, sin herencia de excepción: AnalizadorSintacticoImpl ya no lanza
// estos objetos, los acumula en una lista (ver error()/sincronizar(), REQ-AS-008)
// para poder reportar más de uno por corrida. Sin lineaFuente (a diferencia de
// ErrorLexico) porque Token no lleva columna, así que no hay nada que subrayar.
public final class ErrorSintactico {

    private final int linea;
    private final String lexema;
    private final String encontrado;
    private final String esperado;

    public ErrorSintactico(int linea, String lexema, String encontrado, String esperado) {
        this.linea = linea;
        this.lexema = lexema;
        this.encontrado = encontrado;
        this.esperado = esperado;
    }

    public int getLinea() {
        return linea;
    }

    // Texto crudo del token ofensivo (para la etiqueta [Error:<lexema>|<linea>]).
    public String getLexema() {
        return lexema;
    }

    public String getEncontrado() {
        return encontrado;
    }

    public String getEsperado() {
        return esperado;
    }
}
