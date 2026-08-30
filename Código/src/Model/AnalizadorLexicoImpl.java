package Model;

import java.io.IOException;

// Lee directamente de SourceManager (sin pasar por LexicoHandler): ese es el
// camino caliente, ejecutado una vez por carácter, y no debe atravesar el controller.
// REQ-AL-01: un método por estado del AF.
public class AnalizadorLexicoImpl implements AnalizadorLexico
{

    private final SourceManager sourceManager;
    private final ResultadoLexicoListener listener;

    // Único carácter de lookahead para operadores compuestos (<, <=, &&, //, /*, ++, etc.)
    // sin necesidad de expandir la interfaz SourceManager con unread/peek.
    private char caracterActual;
    private final StringBuilder lexema;
    private int nroColumna; //identificar posición del error.
    Token tokenAnalizado;

    public AnalizadorLexicoImpl(SourceManager sourceManager, ResultadoLexicoListener listener)
    {
        this.sourceManager = sourceManager;
        this.listener = listener;
        this.lexema = new StringBuilder();
    }

    private void actualizarLexema()
    {
        lexema.append(caracterActual);
    }

    private void actualizarCaracterActual() throws IOException
    {
        boolean eraSaltoDeLinea = caracterActual == '\n' || caracterActual == '\r';
        caracterActual = sourceManager.getNextChar();
        if(eraSaltoDeLinea)
        {
            actualizarLinea();
        }
        nroColumna++;
    }

    private void actualizarLinea()
    {
        nroColumna = 0;
    }

    private void armarToken(TokenType tipo)
    {
        tokenAnalizado = new Token(tipo, lexema.toString(), sourceManager.getLineNumber());
    }

    private void emitir(TokenType tipo) throws IOException
    {
        actualizarLexema();
        armarToken(tipo);
        actualizarCaracterActual();
    }

    // "lexemaError" es lo que se reporta como texto ofensivo: un solo carácter para
    // símbolos sueltos inválidos, o el lexema parcial acumulado (lexema.toString())
    // cuando el error corta a mitad de un token de varios caracteres (string, char,
    // exponente, &/| sueltos), para que el mensaje apunte a algo útil y no a
    // cualquier carácter que haya quedado de lookahead.
    private void reportarError(String lexemaError, String razon)
    {
        reportarError(lexemaError, razon, nroColumna);
    }

    // Variante con columna explícita: para los estados que acumulan varios
    // caracteres en un while y sólo deciden que hay error *después* de haber
    // llamado actualizarCaracterActual() sobre el último de ellos, nroColumna ya
    // quedó apuntando al carácter siguiente (el lookahead), no al último
    // carácter del lexema ofensivo. Esos casos deben pasar "nroColumna - 1".
    private void reportarError(String lexemaError, String razon, int columna)
    {
        listener.onError(new ErrorLexico(sourceManager.getLineNumber(), columna, lexemaError, razon, sourceManager.getLineaActual()));
    }

    @Override
    public void startAnalizar() throws IOException
    {
        // Loop principal: mientras no se llegue a EOF, invoca estadoInicial() por cada
        // token, emite vía listener.onToken(...) / listener.onError(...), y no se
        // detiene ante el primer error (REQ-AL-22).
        actualizarCaracterActual();

        while(caracterActual != SourceManager.END_OF_FILE)
        {
            lexema.setLength(0);
            tokenAnalizado = null;

            estadoInicial();

            if(tokenAnalizado != null)
            {
                listener.onToken(tokenAnalizado);
            }
        }

        lexema.setLength(0);
        lexema.append('$');
        armarToken(TokenType.EOF);
        listener.onToken(tokenAnalizado);
    }

    private void estadoInicial() throws IOException
    {
        if(Character.isWhitespace(caracterActual))
        {
            actualizarCaracterActual();
        }
        else if (Character.isUpperCase(caracterActual))
        {
            estadoIdGenOClass();
        }
        else if (Character.isLowerCase(caracterActual))
        {
            estadoIdMetVar();
        }
        else if (Character.isDigit(caracterActual))
        {
            estadoNumero();
        }
        else
        {
            switch (caracterActual)
            {
                case '(' -> emitir(TokenType.PAR_A);
                case ')' -> emitir(TokenType.PAR_C);
                case '{' -> emitir(TokenType.LLAVE_A);
                case '}' -> emitir(TokenType.LLAVE_C);
                case '[' -> emitir(TokenType.CORCHETE_A);
                case ']' -> emitir(TokenType.CORCHETE_C);
                case ';' -> emitir(TokenType.PUNTO_COMA);
                case ',' -> emitir(TokenType.COMA);
                case '.' -> emitir(TokenType.PUNTO);
                case ':' -> emitir(TokenType.DOS_PUNTOS);
                case '%' -> emitir(TokenType.OP_MOD);
                case '*' -> emitir(TokenType.OP_MULT);
                case '\'' -> estadoChar();      // REQ-AL-10
                case '"' -> estadoString();     // REQ-AL-11
                case '<' -> estadoMenor();      // necesita ver si sigue '=' -> <=
                case '>' -> estadoMayor();      // idem -> >=
                case '=' -> estadoIgual();      // idem -> ==
                case '!' -> estadoNot();        // idem -> !=
                case '&' -> estadoAnd();        // espera el segundo '&'
                case '|' -> estadoOr();
                case '+' -> estadoMas();        // idem -> ++
                case '-' -> estadoMenos();      // idem -> --
                case '/' -> estadoDiv();        // idem -> // o /*
                default -> estadoCaracterInvalido();
            }
        }
    }

    // REQ-AL-19/22: reporta y, fundamental, avanza. Sin el avance el carácter
    // inválido queda para siempre como caracterActual y el analizador no termina.
    private void estadoCaracterInvalido() throws IOException
    {
        reportarError(String.valueOf(caracterActual), "no es un símbolo válido");
        actualizarCaracterActual();
    }

    // REQ-AL-04/05: idGen es una única mayúscula; idClase es una mayúscula seguida
    // de uno o más (letra/dígito/'_'). El cierre no depende de que venga un blanco:
    // cualquier carácter que no continúe el identificador lo cierra (maximal munch).
    private void estadoIdGenOClass() throws IOException
    {
        actualizarLexema();
        actualizarCaracterActual();
        if(Character.isLetterOrDigit(caracterActual) || caracterActual == '_')
        {
            estadoIdClase();
        }
        else
        {
            armarToken(TokenType.ID_GEN);
        }
    }

    private void estadoIdClase() throws IOException
    {
        while(Character.isLetterOrDigit(caracterActual) || caracterActual == '_')
        {
            actualizarLexema();
            actualizarCaracterActual();
        }
        armarToken(TokenType.ID_CLASE);
    }

    private void estadoNumero() throws IOException
    {
        estadoEntero();
    }

    private void estadoEntero() throws IOException
    {
        while(Character.isDigit(caracterActual))
        {
            actualizarLexema();
            actualizarCaracterActual();
        }

        if(caracterActual == '.')
        {
            actualizarLexema();
            actualizarCaracterActual();
            estadoParteDecimal();
        }
        else if(caracterActual == 'e' || caracterActual == 'E')
        {
            actualizarLexema();
            actualizarCaracterActual();
            estadoParteExponente();
        }
        else if(Character.isLetter(caracterActual) || caracterActual == '_')
        {
            consumirSufijoInvalidoDeNumero();
        }
        else if(lexema.length() > 9)
        {
            reportarError(lexema.toString(), "el literal entero supera los 9 dígitos permitidos", nroColumna - 1);
        }
        else
        {
            armarToken(TokenType.LIT_INT);
        }
    }

    // REQ-AL-09/REQ-AL-21: un literal numérico exige empezar con dígito, no
    // prohíben explícitamente terminar en letra, pero sin este chequeo "345a" se
    // partiría silenciosamente en intLiteral(345) + idMV(a). Se consume el sufijo
    // completo y se reporta como un único lexema inválido en vez de partirlo.
    private void consumirSufijoInvalidoDeNumero() throws IOException
    {
        while(Character.isLetterOrDigit(caracterActual) || caracterActual == '_')
        {
            actualizarLexema();
            actualizarCaracterActual();
        }
        reportarError(lexema.toString(), "un literal numérico no puede ir seguido inmediatamente de una letra o guion bajo", nroColumna - 1);
    }

    // Parte decimal: ya se consumió el '.'. No exige dígitos después (Java acepta
    // "3." como float válido), así que solo se queda mientras sigan llegando dígitos.
    // Si aparece 'e'/'E' pasa al exponente; cualquier otro carácter cierra como float.
    private void estadoParteDecimal() throws IOException
    {
        while(Character.isDigit(caracterActual))
        {
            actualizarLexema();
            actualizarCaracterActual();
        }

        if(caracterActual == 'e' || caracterActual == 'E')
        {
            actualizarLexema();
            actualizarCaracterActual();
            estadoParteExponente();
        }
        else
        {
            cerrarLiteralFloat();
        }
    }

    // Parte exponente: ya se consumió 'e'/'E'. Acepta un signo opcional, pero a
    // diferencia de la parte decimal acá sí hace falta al menos un dígito después
    // (ni Java acepta "3e" o "3e+" solos), por eso el primer dígito obligatorio se
    // controla acá y los siguientes, opcionales, en estadoDigitosExponente.
    //no esta generando token.
    private void estadoParteExponente() throws IOException
    {
        if(caracterActual == '+' || caracterActual == '-')
        {
            actualizarLexema();
            actualizarCaracterActual();
        }

        if(Character.isDigit(caracterActual))
        {
            estadoDigitosExponente();
        }
        else
        {
            reportarError(lexema.toString(), "se esperaba al menos un dígito en el exponente");
            actualizarCaracterActual();
        }
    }

    private void estadoDigitosExponente() throws IOException
    {
        while(Character.isDigit(caracterActual))
        {
            actualizarLexema();
            actualizarCaracterActual();
        }
        cerrarLiteralFloat();
    }

    // REQ-AL-21: "misma estructura que los floats de Java" incluye el sufijo de
    // tipo f/F/d/D (FloatTypeSuffix de la gramática de Java), aun cuando miniJava
    // no distinga float de double como tipos declarables (no hay keyword para
    // ninguno de los dos): se acepta y se descarta como parte del lexema, sin
    // armar un TokenType distinto. Cualquier otra letra o '_' pegada después
    // sigue siendo inválida, con el mismo criterio que en los enteros.
    private void cerrarLiteralFloat() throws IOException
    {
        if(esSufijoFloatValido(caracterActual))
        {
            actualizarLexema();
            actualizarCaracterActual();
        }

        if(Character.isLetterOrDigit(caracterActual) || caracterActual == '_')
        {
            consumirSufijoInvalidoDeNumero();
        }
        else
        {
            armarToken(TokenType.LIT_FLOAT);
        }
    }

    private boolean esSufijoFloatValido(char c)
    {
        return c == 'f' || c == 'F' || c == 'd' || c == 'D';
    }

    // REQ-AL-06: minúscula seguida de 0 o más (letra/dígito/'_'). Cierra ante
    // cualquier carácter que no continúe el identificador; no es un error, es el
    // caso normal de "no hay más caracteres del token" (0 caracteres extra es válido).
    // REQ-AL-03/07: antes de armar ID_MET_VAR se consulta la tabla de palabras clave;
    // todas empiezan con minúscula, así que este es el único estado que puede chocar.
    private void estadoIdMetVar() throws IOException
    {
        while(Character.isLetterOrDigit(caracterActual) || caracterActual == '_')
        {
            actualizarLexema();
            actualizarCaracterActual();
        }

        TokenType palabraClave = TablaPalabrasClave.resolver(lexema.toString());
        armarToken(palabraClave != null ? palabraClave : TokenType.ID_MET_VAR);
    }

    // Operadores compuestos: se arma el token corto apenas se ve que el carácter
    // siguiente no continúa el compuesto, sin importar cuál sea (blanco, operador,
    // puntuación o EOF); no hace falta lookahead especial para blancos.
    private void estadoMenor() throws IOException
    {
        actualizarLexema();
        actualizarCaracterActual();
        if(caracterActual == '=')
        {
            actualizarLexema();
            actualizarCaracterActual();
            armarToken(TokenType.OP_MENOR_IGUAL);
        }
        else
        {
            armarToken(TokenType.OP_MENOR);
        }
    }

    private void estadoMayor() throws IOException
    {
        actualizarLexema();
        actualizarCaracterActual();
        if(caracterActual == '=')
        {
            actualizarLexema();
            actualizarCaracterActual();
            armarToken(TokenType.OP_MAYOR_IGUAL);
        }
        else
        {
            armarToken(TokenType.OP_MAYOR);
        }
    }

    private void estadoIgual() throws IOException
    {
        actualizarLexema();
        actualizarCaracterActual();
        if(caracterActual == '=')
        {
            actualizarLexema();
            actualizarCaracterActual();
            armarToken(TokenType.OP_IGUAL);
        }
        else
        {
            armarToken(TokenType.OP_ASIGN);
        }
    }

    private void estadoNot() throws IOException
    {
        actualizarLexema();
        actualizarCaracterActual();
        if(caracterActual == '=')
        {
            actualizarLexema();
            actualizarCaracterActual();
            armarToken(TokenType.OP_DISTINTO);
        }
        else
        {
            armarToken(TokenType.OP_NOT);
        }
    }

    private void estadoAnd() throws IOException
    {
        actualizarLexema();
        actualizarCaracterActual();
        if(caracterActual == '&')
        {
            actualizarLexema();
            actualizarCaracterActual();
            armarToken(TokenType.OP_AND);
        }
        else
        {
            reportarError(lexema.toString(), "se esperaba '&' para formar el operador &&", nroColumna - 1);
        }
    }

    private void estadoOr() throws IOException
    {
        actualizarLexema();
        actualizarCaracterActual();
        if(caracterActual == '|')
        {
            actualizarLexema();
            actualizarCaracterActual();
            armarToken(TokenType.OP_OR);
        }
        else
        {
            reportarError(lexema.toString(), "se esperaba '|' para formar el operador ||", nroColumna - 1);
        }
    }

    private void estadoMas() throws IOException
    {
        actualizarLexema();
        actualizarCaracterActual();
        if(caracterActual == '+')
        {
            actualizarLexema();
            actualizarCaracterActual();
            armarToken(TokenType.OP_INCREMENTO);
        }
        else
        {
            armarToken(TokenType.OP_MAS);
        }
    }

    private void estadoMenos() throws IOException
    {
        actualizarLexema();
        actualizarCaracterActual();
        if(caracterActual == '-')
        {
            actualizarLexema();
            actualizarCaracterActual();
            armarToken(TokenType.OP_DECREMENTO);
        }
        else
        {
            armarToken(TokenType.OP_MENOS);
        }
    }

    // '/' solo -> OP_DIV; '//' -> comentario de línea; '/*' -> comentario de bloque.
    // REQ-AL-18: los comentarios no son tokens, se descartan sin armar tokenAnalizado.
    private void estadoDiv() throws IOException
    {
        actualizarLexema();
        actualizarCaracterActual();
        if(caracterActual == '/')
        {
            actualizarCaracterActual();
            estadoComentarioLinea();
        }
        else if(caracterActual == '*')
        {
            actualizarCaracterActual();
            estadoComentarioBloque();
        }
        else
        {
            armarToken(TokenType.OP_DIV);
        }
    }

    // Descarta hasta el fin de línea (o EOF); el salto de línea en sí lo consume
    // el chequeo de whitespace de estadoInicial en la próxima vuelta del loop.
    private void estadoComentarioLinea() throws IOException
    {
        while(caracterActual != '\n' && caracterActual != '\r' && caracterActual != SourceManager.END_OF_FILE)
        {
            actualizarCaracterActual();
        }
    }

    // Descarta hasta encontrar "*/"; si llega a EOF sin cerrar, es un error léxico.
    private void estadoComentarioBloque() throws IOException
    {
        while(true)
        {
            if(caracterActual == SourceManager.END_OF_FILE)
            {
                reportarError("/*", "comentario de bloque sin cerrar");
                return;
            }

            if(caracterActual == '*')
            {
                actualizarCaracterActual();
                if(caracterActual == '/')
                {
                    actualizarCaracterActual();
                    return;
                }
            }
            else
            {
                actualizarCaracterActual();
            }
        }
    }

    // REQ-AL-10: 'x' con x cualquier carácter salvo '\' o '\''; o '\x' (escape) con
    // x cualquier carácter. Un literal vacío ('') o sin cerrar es error léxico.
    private void estadoChar() throws IOException
    {
        actualizarLexema();
        actualizarCaracterActual();

        if(caracterActual == '\\')
        {
            consumirEscape();
        }
        else if(caracterActual == '\'' || caracterActual == SourceManager.END_OF_FILE
                || caracterActual == '\n' || caracterActual == '\r')
        {
            reportarError(lexema.toString(), "se esperaba un carácter dentro del literal de carácter");
            return;
        }
        else
        {
            actualizarLexema();
            actualizarCaracterActual();
        }

        if(caracterActual == '\'')
        {
            actualizarLexema();
            actualizarCaracterActual();
            armarToken(TokenType.LIT_CHAR);
        }
        else
        {
            // Si lo que sigue es un carácter real e imprimible (ej. la 'b' de
            // 'ab'), apunta ahí: es el carácter que debería haber sido la
            // comilla de cierre y no lo fue. Pero si lo que falta es EOF o un
            // salto de línea real, ahí no hay nada que señalar en la misma
            // línea de Detalle: en ese caso se cae al último carácter real del
            // lexema (nroColumna - 1), igual que en estadoEntero/estadoAnd/
            // estadoOr, para no dejar el ^ apuntando al vacío.
            int columna = (caracterActual == SourceManager.END_OF_FILE
                    || caracterActual == '\n' || caracterActual == '\r')
                    ? nroColumna - 1
                    : nroColumna;
            reportarError(lexema.toString(), "falta la comilla simple de cierre", columna);
        }
    }

    // REQ-AL-11: "..." sin saltos de línea sin escapar; una '"' precedida por '\'
    // no cierra el string. REQ-AL-20: una barra invertida seguida de un carácter
    // (o de "u" + 4 hex, escape Unicode) cuenta como un único carácter lógico.
    private void estadoString() throws IOException
    {
        actualizarLexema();
        actualizarCaracterActual();

        while(caracterActual != '"' && caracterActual != SourceManager.END_OF_FILE
                && caracterActual != '\n' && caracterActual != '\r')
        {
            if(caracterActual == '\\')
            {
                consumirEscape();
            }
            else
            {
                actualizarLexema();
                actualizarCaracterActual();
            }
        }

        if(caracterActual == '"')
        {
            actualizarLexema();
            actualizarCaracterActual();
            armarToken(TokenType.LIT_STRING);
        }
        else
        {
            reportarError(lexema.toString(), "literal de string sin cerrar antes del salto de línea o fin de archivo");
        }
    }

    // Compartido por estadoChar/estadoString. Ya se sabe que caracterActual == '\'.
    private void consumirEscape() throws IOException
    {
        actualizarLexema();
        actualizarCaracterActual();

        if(caracterActual == 'u')
        {
            actualizarLexema();
            actualizarCaracterActual();
            for(int i = 0; i < 4; i++)
            {
                if(esHexadecimal(caracterActual))
                {
                    actualizarLexema();
                    actualizarCaracterActual();
                }
                else
                {
                    reportarError(lexema.toString(), "se esperaban 4 dígitos hexadecimales en el escape unicode");
                    return;
                }
            }
        }
        // REQ-AL-11: un salto de línea (crudo, no escapado como \n) nunca puede
        // formar parte de la secuencia de caracteres. Sin esta exclusión, un
        // string como "abc\<salto real>def" quedaba aceptado silenciosamente
        // como un único literal de dos líneas, tratando el salto de línea como
        // si fuera "el carácter x" de \x. Al no consumirlo acá, se lo deja para
        // que estadoString/estadoChar lo vean como lo que es (fin de línea sin
        // cerrar el literal) y reporten el error correspondiente.
        else if(caracterActual != SourceManager.END_OF_FILE
                && caracterActual != '\n' && caracterActual != '\r')
        {
            actualizarLexema();
            actualizarCaracterActual();
        }
    }

    private boolean esHexadecimal(char c)
    {
        return Character.isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

}
