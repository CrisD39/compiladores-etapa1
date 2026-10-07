package Model.semantico;

// Error semántico de "chequeo de declaraciones": se identifica con un código
// fijo por regla violada (ver "Decisión tomada" en
// propuesta_casos_test_semantico.md), no con un mensaje libre -- así no
// depende de la redacción exacta y permite comparar contra un conjunto de
// constantes compartido con los tests (///[ErrorSem:<codigo>|<línea>]).
public final class ErrorSemantico {

    private final String codigo;
    private final int linea;
    private final String lexema;

    public ErrorSemantico(String codigo, int linea, String lexema) {
        this.codigo = codigo;
        this.linea = linea;
        this.lexema = lexema;
    }

    public String getCodigo() {
        return codigo;
    }

    public int getLinea() {
        return linea;
    }

    // Lexema de la entidad que dispara el error (para el mensaje legible).
    public String getLexema() {
        return lexema;
    }
}
