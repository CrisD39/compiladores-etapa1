package Model;

import java.io.IOException;

// Lee directamente de SourceManager (sin pasar por LexicoHandler): ese es el
// camino caliente, ejecutado una vez por carácter, y no debe atravesar el controller.
// REQ-AL-01: un método por estado del AF. Se deja como esqueleto: el detalle de cada
// estado es la siguiente etapa de trabajo, no la estructura de paquetes.
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
        caracterActual = sourceManager.getNextChar();
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

    private void reportarError(char caracter, String razon)
    {
        // TODO: pasar la línea fuente completa (REQ-MP-08) una vez que SourceManager la exponga.
        listener.onError(new ErrorLexico(sourceManager.getLineNumber(), nroColumna, String.valueOf(caracter), razon, ""));
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

        // REQ-AL-16: el EOF es un token más, con lexema "$".
        lexema.setLength(0);
        lexema.append('$');
        armarToken(TokenType.EOF);
        listener.onToken(tokenAnalizado);
    }

    //TODO: Ver como llegar a estado COmentario.
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
            //Falta agregar el " " para reconcer string?
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
                case '<' -> estadoMenor();      // necesita ver si sigue '=' -> <=
                case '>' -> estadoMayor();      // idem -> >=
                case '=' -> estadoIgual();      // idem -> ==
                case '!' -> estadoNot();        // idem -> !=
                case '&' -> estadoAnd();        // espera el segundo '&'
                case '|' -> estadoOr();
                case '+' -> estadoMas();        // idem -> ++
                case '-' -> estadoMenos();      // idem -> --
                case '/' -> estadoDiv();        // idem -> // o /*
                default -> reportarError(caracterActual, "no es un símbolo válido");
            }
        }
    }

    private void estadoIdGenOClass() throws IOException
    {
        actualizarCaracterActual();
        //reconozco entero
        if(Character.isWhitespace(caracterActual))
            tokenAnalizado = new Token(TokenType.LIT_INT,lexema.toString(),sourceManager.getLineNumber());
        else if(Character.isLetter(caracterActual) || Character.isDigit(caracterActual))
        {
            actualizarLexema();
            actualizarCaracterActual();
        }
            else
                tokenAnalizado = new Token(TokenType.ID_CLASE,lexema.toString(),sourceManager.getLineNumber());
    }

    // REQ-AL-21: los floats tienen la misma estructura que los de Java, pero siempre
    // empiezan con un dígito (nunca con '.'), así que el punto de entrada del número
    // siempre es la parte entera.
    private void estadoNumero() throws IOException
    {
        estadoEntero();
    }

    // Parte entera: mientras haya dígitos se queda acá (y cuenta cuántos lleva, por
    // REQ-AL-09: máximo 9 dígitos para intLiteral). Si aparece '.' pasa a la parte
    // decimal; si aparece 'e'/'E' pasa directo al exponente sin parte decimal (ej:
    // "3e10" es un float válido). Cualquier otro carácter cierra como entero.
    private void estadoEntero() throws IOException
    {
        if(Character.isDigit(caracterActual))
        {
            actualizarLexema();
            actualizarCaracterActual();
            estadoEntero();
        }
        else if(caracterActual == '.')
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
        else if(lexema.length() > 9)
        {
            reportarError(caracterActual, "el literal entero supera los 9 dígitos permitidos");
        }
        else
        {
            armarToken(TokenType.LIT_INT);
        }
    }

    // Parte decimal: ya se consumió el '.'. No exige dígitos después (Java acepta
    // "3." como float válido), así que solo se queda mientras sigan llegando dígitos.
    // Si aparece 'e'/'E' pasa al exponente; cualquier otro carácter cierra como float.
    private void estadoParteDecimal() throws IOException
    {
        if(Character.isDigit(caracterActual))
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
        else
        {
            armarToken(TokenType.LIT_FLOAT);
        }
    }

    // Parte exponente: ya se consumió 'e'/'E'. Acepta un signo opcional, pero a
    // diferencia de la parte decimal acá sí hace falta al menos un dígito después
    // (ni Java acepta "3e" o "3e+" solos), por eso el primer dígito obligatorio se
    // controla acá y los siguientes, opcionales, en estadoDigitosExponente.
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
            reportarError(caracterActual, "se esperaba al menos un dígito en el exponente");
            actualizarCaracterActual();
        }
    }

    private void estadoDigitosExponente() throws IOException
    {
        if(Character.isDigit(caracterActual))
        {
            actualizarLexema();
            actualizarCaracterActual();
            estadoDigitosExponente();
        }
        else
        {
            armarToken(TokenType.LIT_FLOAT);
        }
    }

    // TODO: métodos de estado, uno por cada estado del AF pendiente de implementar.
    // Por ahora solo avanzan un carácter para que el analizador no quede colgado.
    private void estadoIdentificador() throws IOException
    {
        actualizarCaracterActual();

    }

    private void estadoIdMetVar() throws IOException
    {
        actualizarCaracterActual();
    }

    private void estadoMenor() throws IOException
    {
        actualizarCaracterActual();
    }

    private void estadoMayor() throws IOException
    {
        actualizarCaracterActual();
    }

    private void estadoIgual() throws IOException
    {
        actualizarCaracterActual();
    }

    private void estadoNot() throws IOException
    {
        actualizarCaracterActual();
    }

    private void estadoAnd() throws IOException
    {
        actualizarCaracterActual();
    }

    private void estadoOr() throws IOException
    {
        actualizarCaracterActual();
    }

    private void estadoMas() throws IOException
    {
        actualizarCaracterActual();
    }

    private void estadoMenos() throws IOException
    {
        actualizarCaracterActual();
    }

    private void estadoDiv() throws IOException
    {
        actualizarCaracterActual();
    }
    // private void estadoChar() { ... }
    // private void estadoString() {...}
    // private void estadoComentario() { ... }

}
