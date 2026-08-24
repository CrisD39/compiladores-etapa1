package Model;

public final class Token {

    private final TokenType tipo;
    private final String lexema;
    private final int linea;

    public Token(TokenType tipo, String lexema, int linea) {
        this.tipo = tipo;
        this.lexema = lexema;
        this.linea = linea;
    }

    public TokenType getTipo() {
        return tipo;
    }

    public String getLexema() {
        return lexema;
    }

    public int getLinea() {
        return linea;
    }

    // Formato pedido por REQ-MP-04: (Nombre token, lexema, Nro Linea)
    @Override
    public String toString() {
        return "(" + tipo.getNombre() + "," + lexema + "," + linea + ")";
    }
}
