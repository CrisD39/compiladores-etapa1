package Model;

import java.util.HashMap;
import java.util.Map;

// Tabla de palabras clave pedida en REQ-AL-03: lookup O(1) al armar un identificador,
// en vez de una cadena de if/else.
public final class TablaPalabrasClave {

    private static final Map<String, TokenType> PALABRAS_CLAVE = new HashMap<>();

    static {
        PALABRAS_CLAVE.put("class", TokenType.PR_CLASS);
        PALABRAS_CLAVE.put("extends", TokenType.PR_EXTENDS);
        PALABRAS_CLAVE.put("interface", TokenType.PR_INTERFACE);
        PALABRAS_CLAVE.put("implements", TokenType.PR_IMPLEMENTS);
        PALABRAS_CLAVE.put("static", TokenType.PR_STATIC);
        PALABRAS_CLAVE.put("boolean", TokenType.PR_BOOLEAN);
        PALABRAS_CLAVE.put("char", TokenType.PR_CHAR);
        PALABRAS_CLAVE.put("int", TokenType.PR_INT);
        PALABRAS_CLAVE.put("void", TokenType.PR_VOID);
        PALABRAS_CLAVE.put("public", TokenType.PR_PUBLIC);
        PALABRAS_CLAVE.put("if", TokenType.PR_IF);
        PALABRAS_CLAVE.put("else", TokenType.PR_ELSE);
        PALABRAS_CLAVE.put("while", TokenType.PR_WHILE);
        PALABRAS_CLAVE.put("return", TokenType.PR_RETURN);
        PALABRAS_CLAVE.put("var", TokenType.PR_VAR);
        PALABRAS_CLAVE.put("this", TokenType.PR_THIS);
        PALABRAS_CLAVE.put("new", TokenType.PR_NEW);
        PALABRAS_CLAVE.put("null", TokenType.PR_NULL);
        PALABRAS_CLAVE.put("true", TokenType.PR_TRUE);
        PALABRAS_CLAVE.put("false", TokenType.PR_FALSE);
    }

    private TablaPalabrasClave() {
    }

    // Devuelve el TokenType si el lexema es una palabra clave, o null si no lo es.
    public static TokenType resolver(String lexema) {
        return PALABRAS_CLAVE.get(lexema);
    }
}
