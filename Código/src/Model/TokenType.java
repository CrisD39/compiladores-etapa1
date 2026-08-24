package Model;

public enum TokenType {

    // Palabras clave
    PR_CLASS("pr_class"), PR_EXTENDS("pr_extends"), PR_INTERFACE("pr_interface"),
    PR_IMPLEMENTS("pr_implements"), PR_STATIC("pr_static"), PR_BOOLEAN("pr_boolean"),
    PR_CHAR("pr_char"), PR_INT("pr_int"), PR_VOID("pr_void"), PR_PUBLIC("pr_public"),
    PR_IF("pr_if"), PR_ELSE("pr_else"), PR_WHILE("pr_while"), PR_RETURN("pr_return"),
    PR_VAR("pr_var"), PR_THIS("pr_this"), PR_NEW("pr_new"), PR_NULL("pr_null"),
    PR_TRUE("pr_true"), PR_FALSE("pr_false"),

    // Identificadores
    ID_CLASE("idClase"), ID_GEN("idGen"), ID_MET_VAR("idMV"),

    // Literales
    LIT_INT("intLiteral"), LIT_CHAR("charLiteral"), LIT_STRING("litString"), LIT_FLOAT("floatLiteral"),

    // Símbolos de puntuación
    PAR_A("parA"), PAR_C("parC"), LLAVE_A("llaveA"), LLAVE_C("llaveC"),
    CORCHETE_A("corcheteA"), CORCHETE_C("corcheteC"), PUNTO_COMA("puntoYComa"),
    COMA("coma"), PUNTO("punto"), DOS_PUNTOS("dosPuntos"),

    // Operadores
    OP_MAYOR("op>"), OP_MENOR("op<"), OP_NOT("op!"), OP_ASIGN("op="), OP_IGUAL("op=="),
    OP_MAYOR_IGUAL("op>="), OP_MENOR_IGUAL("op<="), OP_DISTINTO("op!="),
    OP_AND("op&&"), OP_OR("op||"), OP_MOD("op%"), OP_MAS("op+"), OP_MENOS("op-"),
    OP_MULT("op*"), OP_DIV("op/"), OP_INCREMENTO("op++"), OP_DECREMENTO("op--"),

    // Fin de archivo
    EOF("EOF");

    private final String nombre;

    TokenType(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
