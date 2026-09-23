package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Analizador sintáctico descendente recursivo predictivo (LL(1)) para MiniJava.
 *
 * <p>Es la traducción mecánica de la gramática de la sección
 * "Gramática LL(1) resultante" de {@code Documentación/analizador_sintactico.md}:
 * un método privado por cada no terminal, con el mismo nombre en minúscula
 * (los que chocan con palabras reservadas de Java se prefijan con
 * {@code sentencia...}: {@code sentenciaIf}, {@code sentenciaWhile},
 * {@code sentenciaReturn}). Cada método:
 * <ul>
 *   <li>llama a {@link #match(TokenType)} por cada terminal de la producción,</li>
 *   <li>llama al método del no terminal por cada no terminal,</li>
 *   <li>elige alternativa mirando {@code tokenActual} contra el conjunto FIRST
 *       de cada rama; el caso {@code ϵ} es el {@code else} que no hace nada.</li>
 * </ul>
 *
 * <p>La expresión lambda ({@code REQ-AS-005}) ya está implementada según la
 * sección "Gramática Expandida (Logros)" del documento: se sumó el token
 * {@code ARROW} ({@code ->}) al léxico y {@code <Operando>} quedó reescrito con
 * la factorización {@code <TrasId>} / {@code <TrasParen>} / {@code <TrasParenId>}
 * / {@code <DecidirTrasCierre>} / {@code <ListaIdLambda>}, que inlinea
 * {@code <Referencia>}, {@code <Primario>} y {@code <ExpresionParentizada>}. El
 * parser sigue decidiendo sólo con FIRST y un token de lookahead ({@code ->} se
 * distingue por su terminal). La bifurcación por {@code idMetVar} en
 * {@code <TrasParenId>} además rechaza en el propio sintáctico casos como
 * {@code (a + b) -> e} (no hace falta diferirlo a una etapa semántica): ningún
 * operando que no arranque con {@code idMetVar} puede ser un parámetro válido.
 *
 * <p>La variable local clásica ({@code REQ-AS-006}) también está implementada:
 * {@code <Sentencia>} suma {@code <TipoPrimitivo>} / {@code idGen} / {@code idClase}
 * como arranque de {@code int x, y, z = 10;} (sin {@code var}), con
 * {@code <SentIdClase>} factorizando el prefijo {@code idClase} (declaración vs.
 * llamada estática) por el token que sigue. La forma con {@code var}
 * ({@code <VarLocal>}) queda intacta.
 *
 * <p>La visibilidad de miembros ({@code REQ-AS-007}) también está implementada:
 * se sumó el token {@code PR_PRIVATE} (palabra clave {@code private}) al léxico
 * y {@code <Miembro>} quedó reescrito como {@code <Visibilidad> <CuerpoMiembro>}
 * ({@code visibilidad()} / {@code cuerpoMiembro()}). Como {@code public} deja de
 * ser el token que marca al constructor, su prefijo {@code idClase} pasa a
 * compartirse con el de un atributo/método de tipo clase y se factoriza en
 * profundidad en {@code trasIdClaseMiembro()} (misma técnica que
 * {@code <SentIdClase>}), mirando si tras {@code idClase} viene {@code (}
 * (constructor) o cualquier otra cosa (tipo clase).
 *
 * <p>El {@code for} ({@code REQ-AS-009}) también está implementado, en sus
 * dos formas (clásico con {@code ;} y for-each con
 * {@code :}): se sumó el token {@code PR_FOR} al léxico (el {@code :} ya
 * existía como {@code DOS_PUNTOS}) y {@code sentenciaFor()} llama a
 * {@code clausulasFor()}, que factoriza el prefijo compartido entre ambas
 * formas ({@code <Tipo> idMetVar}) igual que el resto del parser: consume el
 * tipo y el nombre, y decide con **un** token más ({@code :} vs. {@code =}/{@code ;})
 * en {@code trasIdForTipo()}. El prefijo {@code idClase} vuelve a chocar con
 * una llamada estática como inicialización (p. ej. {@code Fabrica.reset()}) y
 * se resuelve en {@code trasIdClaseFor()}, misma técnica que
 * {@code <SentIdClase>}/{@code trasIdClaseMiembro()}. Las tres secciones
 * (init/cond/act) son opcionales, como en Java ({@code for (;;)}); el cuerpo
 * es {@code <Sentencia>}, no {@code <Bloque>}, igual que {@code <If>}/
 * {@code <While>}. El for-each no admite {@code var} (sólo tipo explícito).
 *
 * <p>Los genéricos anidados y la notación diamante ({@code REQ-AS-010})
 * también están implementados: {@code instanciadoOParametrico()} pasó a
 * recursar en su rama {@code idClase} (llama a {@code tipoGenericoOpcional()}),
 * lo que habilita el anidado (p. ej. {@code Caja<Lista<Item>>}) en todos los
 * lugares que ya usaban {@code <TipoGenericoOpcional>} sin tocarlos. El
 * diamante ({@code new Foo<>()}) es aparte, sólo válido al instanciar: se
 * sumaron {@code tipoGenericoOpcionalNew()} / {@code diamanteOTipo()} (el
 * interior puede ser vacío) y {@code restoNew()} los usa en vez de
 * {@code tipoGenericoOpcional()}; el resto de contextos de tipo sigue exigiendo
 * un argumento real, así que {@code Foo<> x;} sigue siendo error sintáctico.
 * El cierre {@code >>} de un genérico anidado tokeniza como dos {@code OP_MAYOR}
 * sueltos ({@code estadoMayor()} sólo combina con {@code =}), así que no hizo
 * falta ningún cambio en el léxico.
 *
 * <p>Los inicializadores de atributo ({@code REQ-AS-011}) también están
 * implementados: {@code <RestoMiembro>} suma
 * una tercera rama {@code <OperadorAsignacion> <ExpresionCompuesta> ;} (p. ej.
 * {@code int x = 5;}), con FIRST disjunto de las otras dos ({@code ;} / {@code (}
 * / {@code =}) — no hizo falta factorizar nada nuevo, es la primera extensión
 * de esta lista sin conflicto LL(1) que resolver. Ya cubre atributos de
 * arreglo con inicializador {@code new} (p. ej. {@code int[] arr = new int[5];}),
 * porque {@code <DimensionesOpcionales>} y {@code <ExpresionCompuesta>} ya
 * existían; lo que falta ({@code REQ-AS-012}) es el inicializador entre llaves
 * ({@code int[] arr = {1, 2, 3};}). Nota de alcance: {@code static} en esta
 * gramática sigue produciendo sólo métodos, nunca atributos (no forma parte de
 * este requerimiento).
 *
 * <p>El inicializador de arreglo entre llaves ({@code REQ-AS-012}) también
 * está implementado, sólo al construir con {@code new}: {@code <DimensionesNew>}
 * reemplaza a la vieja {@code <DimensionesConTamanio>} en {@code restoNew()} /
 * {@code restoNewIdClase()}. Tras el primer {@code [}, un {@code ]} inmediato
 * ({@code trasCorcheteNew()}) compromete a la forma de inicializador —todos los
 * corchetes de esa creación quedan vacíos ({@code masCorchetesVaciosNew()}) y
 * el {@code { ... }} ({@code inicializadorArreglo()}) pasa a ser obligatorio—,
 * mientras que una expresión ahí sigue la forma de tamaño de siempre
 * (reusando {@code dimensionesConTamanioOpc()} sin cambios); no se pueden
 * mezclar ambas, igual que en Java. {@code <ValorArreglo>} admite anidar otro
 * {@code <InicializadorArreglo>}, así que {@code new int[][]{{1,2},{3,4}}} sale
 * gratis. Sin tokens nuevos en el léxico: las llaves ya existían como
 * {@code LLAVE_A}/{@code LLAVE_C} para {@code <Bloque>}.
 *
 * <p>El ternario ({@code REQ-AS-013}) y el postfijo {@code ++}/{@code --}
 * ({@code REQ-AS-014}) también están implementados (ver "Precedencia del
 * operador ternario" y "Postfijo {@code ++}/{@code --}" en el documento).
 *
 * <p>La recuperación en modo pánico ({@code REQ-AS-008}) también está
 * implementada: {@link #error} ya no lanza {@link ErrorSintactico}, lo agrega a
 * una lista interna y llama a {@link #sincronizar()}, que descarta tokens hasta
 * encontrar uno de sincronización (punto y coma, llave de apertura o de cierre,
 * o EOF) antes de devolver el control, para poder seguir el análisis y reportar
 * más de un error por corrida. {@link #getErrores()} expone la lista completa
 * recién al terminar {@link #start()} — no hay reporte en streaming tipo
 * {@code ResultadoLexicoListener}, se decidió juntar todo de una.
 *
 * <p>El wiring completo vía {@code Controller.AnalizadorSintacticoHandler} /
 * {@code AnalizadorSintacticoHandlerImpl} también está hecho, análogo a
 * {@code AnalizadorHandler}/{@code AnalizadorHandlerImpl} para el léxico:
 * {@code ModuloPrincipalET2} ya no arma el léxico y el sintáctico directo.
 *
 * <p>Pendiente (ver el documento de diseño): dos conflictos se resuelven por
 * convención en el método, no en la gramática: {@code else} colgante
 * ({@link #elseOpcional}) y {@code [} tras dimensiones
 * ({@link #dimensionesConTamanioOpc}).
 */
public class AnalizadorSintacticoImpl implements AnalizadorSintactico {

    private final AnalizadorLexico lexico;

    /** Único token de lookahead: el próximo sin consumir. */
    private Token tokenActual;

    public AnalizadorSintacticoImpl(AnalizadorLexico lex) {
        this.lexico = lex;
    }

    // ------------------------------------------------------------------
    // Conjuntos FIRST usados para decidir de rama (los demás no terminales
    // se deciden mirando un único terminal, sin necesidad de un conjunto).
    // ------------------------------------------------------------------

    private static final Set<TokenType> PRIMEROS_TIPO_PRIMITIVO = EnumSet.of(
            TokenType.PR_BOOLEAN, TokenType.PR_CHAR, TokenType.PR_INT);

    // FIRST(<Tipo>) = FIRST(<TipoBase>)
    private static final Set<TokenType> PRIMEROS_TIPO = EnumSet.of(
            TokenType.PR_BOOLEAN, TokenType.PR_CHAR, TokenType.PR_INT,
            TokenType.ID_CLASE, TokenType.ID_GEN);

    // FIRST(<TipoMetodo>) = FIRST(<Tipo>) ∪ { void }
    private static final Set<TokenType> PRIMEROS_TIPO_METODO = EnumSet.of(
            TokenType.PR_BOOLEAN, TokenType.PR_CHAR, TokenType.PR_INT,
            TokenType.ID_CLASE, TokenType.ID_GEN, TokenType.PR_VOID);

    // FIRST(<Miembro>) = FIRST(<Visibilidad>) ∪ FIRST(<CuerpoMiembro>)
    //                  = { public, private } ∪ { static, void } ∪ FIRST(<Tipo>)
    private static final Set<TokenType> PRIMEROS_MIEMBRO = EnumSet.of(
            TokenType.PR_PUBLIC, TokenType.PR_PRIVATE, TokenType.PR_STATIC, TokenType.PR_VOID,
            TokenType.PR_BOOLEAN, TokenType.PR_CHAR, TokenType.PR_INT,
            TokenType.ID_CLASE, TokenType.ID_GEN);

    private static final Set<TokenType> PRIMEROS_PRIMITIVO = EnumSet.of(
            TokenType.PR_TRUE, TokenType.PR_FALSE, TokenType.LIT_INT,
            TokenType.LIT_CHAR, TokenType.PR_NULL);

    private static final Set<TokenType> PRIMEROS_OP_UNARIO = EnumSet.of(
            TokenType.OP_MAS, TokenType.OP_MENOS, TokenType.OP_NOT);

    private static final Set<TokenType> PRIMEROS_OP_BINARIO = EnumSet.of(
            TokenType.OP_OR, TokenType.OP_AND, TokenType.OP_IGUAL, TokenType.OP_DISTINTO,
            TokenType.OP_MENOR, TokenType.OP_MAYOR, TokenType.OP_MENOR_IGUAL,
            TokenType.OP_MAYOR_IGUAL, TokenType.OP_MAS, TokenType.OP_MENOS,
            TokenType.OP_MULT, TokenType.OP_DIV, TokenType.OP_MOD);

    // FIRST(<Operando>) = FIRST(<Primitivo>) ∪ FIRST(<Referencia>)
    private static final Set<TokenType> PRIMEROS_OPERANDO = EnumSet.of(
            TokenType.PR_TRUE, TokenType.PR_FALSE, TokenType.LIT_INT,
            TokenType.LIT_CHAR, TokenType.PR_NULL,
            TokenType.PR_THIS, TokenType.LIT_STRING, TokenType.ID_MET_VAR,
            TokenType.PR_NEW, TokenType.ID_CLASE, TokenType.PAR_A);

    // FIRST(<Expresion>) = FIRST(<OperadorUnario>) ∪ FIRST(<Operando>)
    private static final Set<TokenType> PRIMEROS_EXPRESION = EnumSet.of(
            TokenType.OP_MAS, TokenType.OP_MENOS, TokenType.OP_NOT,
            TokenType.PR_TRUE, TokenType.PR_FALSE, TokenType.LIT_INT,
            TokenType.LIT_CHAR, TokenType.PR_NULL,
            TokenType.PR_THIS, TokenType.LIT_STRING, TokenType.ID_MET_VAR,
            TokenType.PR_NEW, TokenType.ID_CLASE, TokenType.PAR_A);

    // FIRST(<Sentencia>) = { ; var return if while for { } ∪ FIRST(<Tipo>) ∪ FIRST(<Expresion>)
    // FIRST(<Tipo>) suma boolean/char/int e idGen (idClase ya viene de FIRST(<Expresion>)):
    // son el arranque de la declaración de variable local clásica (REQ-AS-006).
    private static final Set<TokenType> PRIMEROS_SENTENCIA = EnumSet.of(
            TokenType.PUNTO_COMA, TokenType.PR_VAR, TokenType.PR_RETURN,
            TokenType.PR_IF, TokenType.PR_WHILE, TokenType.PR_FOR, TokenType.LLAVE_A,
            TokenType.PR_BOOLEAN, TokenType.PR_CHAR, TokenType.PR_INT, TokenType.ID_GEN,
            TokenType.OP_MAS, TokenType.OP_MENOS, TokenType.OP_NOT,
            TokenType.PR_TRUE, TokenType.PR_FALSE, TokenType.LIT_INT,
            TokenType.LIT_CHAR, TokenType.PR_NULL,
            TokenType.PR_THIS, TokenType.LIT_STRING, TokenType.ID_MET_VAR,
            TokenType.PR_NEW, TokenType.ID_CLASE, TokenType.PAR_A);

    // No es un FIRST: es el conjunto de sincronización del modo pánico (REQ-AS-008).
    // Son justo los tokens que ya usan listaSentencias()/listaMiembros()/listaClases()/
    // bloque() para decidir si siguen o cortan — ver sincronizar().
    private static final Set<TokenType> TOKENS_SINCRONIZACION = EnumSet.of(
            TokenType.PUNTO_COMA, TokenType.LLAVE_A, TokenType.LLAVE_C, TokenType.EOF);

    // ------------------------------------------------------------------
    // Infraestructura: start / match / lookahead / error
    // ------------------------------------------------------------------

    private final List<ErrorSintactico> errores = new ArrayList<>();

    @Override
    public void start() {
        tokenActual = lexico.nextToken();
        inicial();
        // REQ-AS-004: <Inicial> ya consume el eof con su match(EOF) final.
    }

    @Override
    public List<ErrorSintactico> getErrores() {
        return Collections.unmodifiableList(errores);
    }

    /** Consume {@code tokenActual} si es del tipo esperado; si no, error sintáctico. */
    private void match(TokenType esperado) {
        if (tokenActual.getTipo() == esperado) {
            avanzar();
        } else {
            error("\"" + esperado.getNombre() + "\"");
        }
    }

    /** Pide el próximo token al léxico. Al llegar a EOF sigue devolviendo EOF. */
    private void avanzar() {
        tokenActual = lexico.nextToken();
    }

    private boolean actualEs(TokenType t) {
        return tokenActual.getTipo() == t;
    }

    private boolean actualEn(Set<TokenType> conjunto) {
        return conjunto.contains(tokenActual.getTipo());
    }

    /**
     * Reporta el error (lo agrega a {@link #errores}, con el {@code tokenActual}
     * de este momento, ANTES de que {@link #sincronizar()} lo mueva) y recupera en
     * modo pánico (REQ-AS-008) para poder seguir el análisis y encontrar más de
     * un error por corrida.
     */
    private void error(String esperado) {
        String encontrado = tokenActual.getTipo().getNombre()
                + " (\"" + tokenActual.getLexema() + "\")";
        errores.add(new ErrorSintactico(tokenActual.getLinea(), tokenActual.getLexema(), encontrado, esperado));
        sincronizar();
    }

    /**
     * Modo pánico (REQ-AS-008): descarta tokens hasta dejar {@code tokenActual}
     * en un punto seguro para retomar. Si el token ofensivo YA es de
     * sincronización (p. ej. "Foo x = new Foo;" seguido de otro error: el ";"
     * que sigue ya estaba bien puesto) no se descarta de más — si no, un error
     * independiente justo a continuación quedaría tragado por esta recuperación.
     * El ";" se consume (ya delimitó el problema); "{"/"}"/EOF se dejan intactos,
     * porque son los que ya usan listaSentencias()/listaMiembros()/listaClases()/
     * bloque() para decidir si siguen o cortan — consumirlos los dejaría ciegos.
     * Termina siempre: el único caso sin avance es cuando el ofensivo ya es
     * "{"/"}"/EOF, y ahí los bucles de arriba no vuelven a invocar la producción
     * que falló (su propio chequeo de FIRST los detiene antes), así que el
     * desenrolle sin progreso real queda acotado por la profundidad de la
     * gramática; EOF, por su parte, nunca se agota (el léxico lo repite siempre).
     */
    private void sincronizar() {
        if (!actualEn(TOKENS_SINCRONIZACION)) {
            avanzar();
            while (!actualEn(TOKENS_SINCRONIZACION)) {
                avanzar();
            }
        }
        if (actualEs(TokenType.PUNTO_COMA)) {
            avanzar();
        }
    }

    // ==================================================================
    // Un método por no terminal (orden de la gramática LL(1) del documento)
    // ==================================================================

    // <Inicial> ::= <ListaClases> eof
    private void inicial() {
        listaClases();
        match(TokenType.EOF);
    }

    // <ListaClases> ::= <Clase> <ListaClases> | <Interfaz> <ListaClases> | ϵ
    private void listaClases() {
        if (actualEs(TokenType.PR_CLASS)) {
            clase();
            listaClases();
        } else if (actualEs(TokenType.PR_INTERFACE)) {
            interfaz();
            listaClases();
        } else {
            // ϵ — no hace nada (FOLLOW = { eof })
        }
    }

    // <Clase> ::= class idClase <GenericidadOpcional> <HerenciaOpcional> { <ListaMiembros> }
    private void clase() {
        match(TokenType.PR_CLASS);
        match(TokenType.ID_CLASE);
        genericidadOpcional();
        herenciaOpcional();
        match(TokenType.LLAVE_A);
        listaMiembros();
        match(TokenType.LLAVE_C);
    }

    // <Interfaz> ::= interface idClase <GenericidadOpcional> <ExtensionOpcional> { <ListaMetodosInterfaz> }
    private void interfaz() {
        match(TokenType.PR_INTERFACE);
        match(TokenType.ID_CLASE);
        genericidadOpcional();
        extensionOpcional();
        match(TokenType.LLAVE_A);
        listaMetodosInterfaz();
        match(TokenType.LLAVE_C);
    }

    // <GenericidadOpcional> ::= < idGen > | ϵ
    private void genericidadOpcional() {
        if (actualEs(TokenType.OP_MENOR)) {
            match(TokenType.OP_MENOR);
            match(TokenType.ID_GEN);
            match(TokenType.OP_MAYOR);
        } else {
            // ϵ — no hace nada
        }
    }

    // <HerenciaOpcional> ::= extends <TipoReferencia> | implements <TipoReferencia> | ϵ
    private void herenciaOpcional() {
        if (actualEs(TokenType.PR_EXTENDS)) {
            match(TokenType.PR_EXTENDS);
            tipoReferencia();
        } else if (actualEs(TokenType.PR_IMPLEMENTS)) {
            match(TokenType.PR_IMPLEMENTS);
            tipoReferencia();
        } else {
            // ϵ — no hace nada
        }
    }

    // <ExtensionOpcional> ::= extends <TipoReferencia> | ϵ
    private void extensionOpcional() {
        if (actualEs(TokenType.PR_EXTENDS)) {
            match(TokenType.PR_EXTENDS);
            tipoReferencia();
        } else {
            // ϵ — no hace nada
        }
    }

    // <ListaMiembros> ::= <Miembro> <ListaMiembros> | ϵ
    private void listaMiembros() {
        if (actualEn(PRIMEROS_MIEMBRO)) {
            miembro();
            listaMiembros();
        } else {
            // ϵ — no hace nada
        }
    }

    // <ListaMetodosInterfaz> ::= <MetodoInterfaz> <ListaMetodosInterfaz> | ϵ
    private void listaMetodosInterfaz() {
        if (actualEn(PRIMEROS_TIPO_METODO)) {
            metodoInterfaz();
            listaMetodosInterfaz();
        } else {
            // ϵ — no hace nada
        }
    }

    // <Miembro> ::= <Visibilidad> <CuerpoMiembro>
    private void miembro() {
        visibilidad();
        cuerpoMiembro();
    }

    // <Visibilidad> ::= public | private | ϵ
    private void visibilidad() {
        if (actualEs(TokenType.PR_PUBLIC)) {
            match(TokenType.PR_PUBLIC);
        } else if (actualEs(TokenType.PR_PRIVATE)) {
            match(TokenType.PR_PRIVATE);
        } else {
            // ϵ — no hace nada
        }
    }

    // <CuerpoMiembro> ::= static <TipoMetodo> idMetVar <ArgsFormales> <Bloque>
    //                 |  void idMetVar <ArgsFormales> <Bloque>
    //                 |  <TipoPrimitivo> <DimensionesOpcionales> idMetVar <RestoMiembro>
    //                 |  idGen <DimensionesOpcionales> idMetVar <RestoMiembro>
    //                 |  idClase <TrasIdClaseMiembro>
    // El "public" que antes marcaba al constructor pasó a ser parte de
    // <Visibilidad>; el constructor (idClase <ArgsFormales> <Bloque>) queda con
    // el mismo prefijo "idClase" que un atributo/método de tipo clase, así que
    // se factoriza en profundidad en trasIdClaseMiembro() (ver "Factorización
    // de <Miembro>" en el documento).
    private void cuerpoMiembro() {
        if (actualEs(TokenType.PR_STATIC)) {
            match(TokenType.PR_STATIC);
            tipoMetodo();
            match(TokenType.ID_MET_VAR);
            argsFormales();
            bloque();
        } else if (actualEs(TokenType.PR_VOID)) {
            match(TokenType.PR_VOID);
            match(TokenType.ID_MET_VAR);
            argsFormales();
            bloque();
        } else if (actualEn(PRIMEROS_TIPO_PRIMITIVO)) {
            tipoPrimitivo();
            dimensionesOpcionales();
            match(TokenType.ID_MET_VAR);
            restoMiembro();
        } else if (actualEs(TokenType.ID_GEN)) {
            match(TokenType.ID_GEN);
            dimensionesOpcionales();
            match(TokenType.ID_MET_VAR);
            restoMiembro();
        } else if (actualEs(TokenType.ID_CLASE)) {
            match(TokenType.ID_CLASE);
            trasIdClaseMiembro();
        } else {
            error("un miembro de clase (\"static\", \"void\" o un tipo)");
        }
    }

    // <TrasIdClaseMiembro> ::= <ArgsFormales> <Bloque>
    //                       |  <TipoGenericoOpcional> <DimensionesOpcionales> idMetVar <RestoMiembro>
    // Tras "idClase", "(" => era el constructor (idClase <ArgsFormales> <Bloque>);
    // cualquier otra cosa => era un atributo o método cuyo tipo es esa clase
    // (Foo bar; | Foo<X> bar; | Foo bar(...) {...}), con <TipoGenericoOpcional>
    // y <DimensionesOpcionales> anulables hasta idMetVar.
    private void trasIdClaseMiembro() {
        if (actualEs(TokenType.PAR_A)) {
            argsFormales();
            bloque();
        } else {
            tipoGenericoOpcional();
            dimensionesOpcionales();
            match(TokenType.ID_MET_VAR);
            restoMiembro();
        }
    }

    // <RestoMiembro> ::= ;
    //                |  <ArgsFormales> <Bloque>
    //                |  <OperadorAsignacion> <ExpresionCompuesta> ;
    // La tercera rama (REQ-AS-011) es un atributo con inicializador
    // (int x = 5;). FIRST disjuntos con las otras dos ({ ; } / { ( } / { = }):
    // no hace falta factorizar nada, "=" ya alcanza para decidir.
    private void restoMiembro() {
        if (actualEs(TokenType.PUNTO_COMA)) {
            match(TokenType.PUNTO_COMA);
        } else if (actualEs(TokenType.PAR_A)) {
            argsFormales();
            bloque();
        } else if (actualEs(TokenType.OP_ASIGN)) {
            operadorAsignacion();
            expresionCompuesta();
            match(TokenType.PUNTO_COMA);
        } else {
            error("\";\", \"=\" (atributo) o \"(\" (método)");
        }
    }

    // <MetodoInterfaz> ::= <TipoMetodo> idMetVar <ArgsFormales> ;
    private void metodoInterfaz() {
        tipoMetodo();
        match(TokenType.ID_MET_VAR);
        argsFormales();
        match(TokenType.PUNTO_COMA);
    }

    // <TipoMetodo> ::= <Tipo> | void
    private void tipoMetodo() {
        if (actualEs(TokenType.PR_VOID)) {
            match(TokenType.PR_VOID);
        } else if (actualEn(PRIMEROS_TIPO)) {
            tipo();
        } else {
            error("un tipo o \"void\"");
        }
    }

    // <Tipo> ::= <TipoBase> <DimensionesOpcionales>
    private void tipo() {
        tipoBase();
        dimensionesOpcionales();
    }

    // <TipoBase> ::= <TipoPrimitivo> | <TipoReferencia> | idGen
    private void tipoBase() {
        if (actualEn(PRIMEROS_TIPO_PRIMITIVO)) {
            tipoPrimitivo();
        } else if (actualEs(TokenType.ID_CLASE)) {
            tipoReferencia();
        } else if (actualEs(TokenType.ID_GEN)) {
            match(TokenType.ID_GEN);
        } else {
            error("un tipo (primitivo, idClase o idGen)");
        }
    }

    // <DimensionesOpcionales> ::= [ ] <DimensionesOpcionales> | ϵ
    private void dimensionesOpcionales() {
        if (actualEs(TokenType.CORCHETE_A)) {
            match(TokenType.CORCHETE_A);
            match(TokenType.CORCHETE_C);
            dimensionesOpcionales();
        } else {
            // ϵ — no hace nada
        }
    }

    // <TipoReferencia> ::= idClase <TipoGenericoOpcional>
    private void tipoReferencia() {
        match(TokenType.ID_CLASE);
        tipoGenericoOpcional();
    }

    // <TipoPrimitivo> ::= boolean | char | int
    private void tipoPrimitivo() {
        if (actualEs(TokenType.PR_BOOLEAN)) {
            match(TokenType.PR_BOOLEAN);
        } else if (actualEs(TokenType.PR_CHAR)) {
            match(TokenType.PR_CHAR);
        } else if (actualEs(TokenType.PR_INT)) {
            match(TokenType.PR_INT);
        } else {
            error("\"boolean\", \"char\" o \"int\"");
        }
    }

    // <TipoGenericoOpcional> ::= < <InstanciadoOParametrico> > | ϵ
    private void tipoGenericoOpcional() {
        if (actualEs(TokenType.OP_MENOR)) {
            match(TokenType.OP_MENOR);
            instanciadoOParametrico();
            match(TokenType.OP_MAYOR);
        } else {
            // ϵ — no hace nada
        }
    }

    // <TipoGenericoOpcionalNew> ::= < <DiamanteOTipo> > | ϵ
    // Variante de <TipoGenericoOpcional> sólo para <RestoNew>: acá sí se
    // admite el interior vacío (REQ-AS-010, notación diamante "<>"), porque
    // el diamante sólo es válido al instanciar (new Foo<>()), nunca en una
    // declaración de tipo — esas siguen usando tipoGenericoOpcional() sin
    // tocar, así que "Foo<> x;" sigue siendo error sintáctico.
    private void tipoGenericoOpcionalNew() {
        if (actualEs(TokenType.OP_MENOR)) {
            match(TokenType.OP_MENOR);
            diamanteOTipo();
            match(TokenType.OP_MAYOR);
        } else {
            // ϵ — no hace nada
        }
    }

    // <DiamanteOTipo> ::= <InstanciadoOParametrico> | ϵ      (ϵ = "<>")
    private void diamanteOTipo() {
        if (actualEs(TokenType.ID_GEN) || actualEs(TokenType.ID_CLASE)) {
            instanciadoOParametrico();
        } else {
            // ϵ — no hace nada (notación diamante: new Foo<>())
        }
    }

    // <InstanciadoOParametrico> ::= idGen | idClase <TipoGenericoOpcional>
    // El idGen no anida (un parámetro de tipo no puede llevar su propio
    // argumento); el idClase sí, vía <TipoGenericoOpcional> (REQ-AS-010:
    // genéricos anidados, p. ej. Caja<Lista<Item>>). El cierre "Item>>" tokeniza
    // como dos OP_MAYOR sueltos (estadoMayor() sólo combina con "="), así que
    // cada nivel de anidamiento consume el suyo sin tratamiento especial.
    private void instanciadoOParametrico() {
        if (actualEs(TokenType.ID_GEN)) {
            match(TokenType.ID_GEN);
        } else if (actualEs(TokenType.ID_CLASE)) {
            match(TokenType.ID_CLASE);
            tipoGenericoOpcional();
        } else {
            error("idGen o idClase");
        }
    }

    // <ArgsFormales> ::= ( <ListaArgsFormalesOpcional> )
    private void argsFormales() {
        match(TokenType.PAR_A);
        listaArgsFormalesOpcional();
        match(TokenType.PAR_C);
    }

    // <ListaArgsFormalesOpcional> ::= <ListaArgsFormales> | ϵ
    private void listaArgsFormalesOpcional() {
        if (actualEn(PRIMEROS_TIPO)) {
            listaArgsFormales();
        } else {
            // ϵ — no hace nada
        }
    }

    // <ListaArgsFormales> ::= <ArgFormal> <ListaArgsFormalesResto>
    private void listaArgsFormales() {
        argFormal();
        listaArgsFormalesResto();
    }

    // <ListaArgsFormalesResto> ::= , <ArgFormal> <ListaArgsFormalesResto> | ϵ
    private void listaArgsFormalesResto() {
        if (actualEs(TokenType.COMA)) {
            match(TokenType.COMA);
            argFormal();
            listaArgsFormalesResto();
        } else {
            // ϵ — no hace nada
        }
    }

    // <ArgFormal> ::= <Tipo> idMetVar
    private void argFormal() {
        tipo();
        match(TokenType.ID_MET_VAR);
    }

    // <Bloque> ::= { <ListaSentencias> }
    private void bloque() {
        match(TokenType.LLAVE_A);
        listaSentencias();
        match(TokenType.LLAVE_C);
    }

    // <ListaSentencias> ::= <Sentencia> <ListaSentencias> | ϵ
    private void listaSentencias() {
        if (actualEn(PRIMEROS_SENTENCIA)) {
            sentencia();
            listaSentencias();
        } else {
            // ϵ — no hace nada
        }
    }

    // <Sentencia> ::= ;
    //             |  <VarLocal> ;                        (var idMetVar = ...)
    //             |  <TipoPrimitivo> <RestoDeclLocal>    \
    //             |  idGen <RestoDeclLocal>              |  variable local clásica (REQ-AS-006)
    //             |  idClase <SentIdClase>              /
    //             |  <Return> ;
    //             |  <If>
    //             |  <While>
    //             |  <Bloque>
    //             |  <Expresion> ;                       (sólo si el lookahead ∈ FIRST(<Expresion>) \ { idClase })
    private void sentencia() {
        if (actualEs(TokenType.PUNTO_COMA)) {
            match(TokenType.PUNTO_COMA);
        } else if (actualEs(TokenType.PR_VAR)) {
            varLocal();
            match(TokenType.PUNTO_COMA);
        } else if (actualEn(PRIMEROS_TIPO_PRIMITIVO)) {
            tipoPrimitivo();
            restoDeclLocal();
        } else if (actualEs(TokenType.ID_GEN)) {
            match(TokenType.ID_GEN);
            restoDeclLocal();
        } else if (actualEs(TokenType.ID_CLASE)) {
            // idClase se intercepta acá: puede abrir una declaración local
            // (<Tipo> idMetVar ...) o una expresión (idClase . idMetVar (...)).
            // Un token más lo decide, en sentIdClase().
            match(TokenType.ID_CLASE);
            sentIdClase();
        } else if (actualEs(TokenType.PR_RETURN)) {
            sentenciaReturn();
            match(TokenType.PUNTO_COMA);
        } else if (actualEs(TokenType.PR_IF)) {
            sentenciaIf();
        } else if (actualEs(TokenType.PR_WHILE)) {
            sentenciaWhile();
        } else if (actualEs(TokenType.PR_FOR)) {
            sentenciaFor();
        } else if (actualEs(TokenType.LLAVE_A)) {
            bloque();
        } else if (actualEn(PRIMEROS_EXPRESION)) {
            // idClase ya quedó tomado por la rama de arriba; acá sólo entra el
            // resto de FIRST(<Expresion>).
            expresion();
            match(TokenType.PUNTO_COMA);
        } else {
            error("una sentencia");
        }
    }

    // <VarLocal> ::= var idMetVar = <ExpresionCompuesta>
    private void varLocal() {
        match(TokenType.PR_VAR);
        match(TokenType.ID_MET_VAR);
        match(TokenType.OP_ASIGN);
        expresionCompuesta();
    }

    // <SentIdClase> ::= <TipoGenericoOpcional> <RestoDeclLocal>
    //               |  . idMetVar <ArgsActuales> <ReferenciaResto> <ExpresionCompuestaResto> <RestoAsignacion> ;
    // El "idClase" ya fue consumido por sentencia(). Si sigue ".", era una
    // llamada a método estático (expresión); si no, es una declaración local
    // con tipo clase (`Foo x;`, `Foo<Bar> x = ...;`). La rama "." reconstruye
    // la cola de <Expresion> que arrancaba en idClase . idMetVar <ArgsActuales>.
    private void sentIdClase() {
        if (actualEs(TokenType.PUNTO)) {
            match(TokenType.PUNTO);
            match(TokenType.ID_MET_VAR);
            argsActuales();
            referenciaResto();
            expresionCompuestaResto();
            restoAsignacion();
            match(TokenType.PUNTO_COMA);
        } else {
            tipoGenericoOpcional();
            restoDeclLocal();
        }
    }

    // <RestoDeclLocal> ::= idMetVar <MasIdsLocal> <InitLocalOpc> ;
    // Cola de una declaración local clásica, después del tipo base. Un único
    // "= <ExpresionCompuesta>" al final vale para toda la lista de nombres
    // (`int x, y, z = 10;`); a qué variables les aplica el valor es semántico.
    private void restoDeclLocal() {
        match(TokenType.ID_MET_VAR);
        masIdsLocal();
        initLocalOpc();
        match(TokenType.PUNTO_COMA);
    }

    // <MasIdsLocal> ::= , idMetVar <MasIdsLocal> | ϵ
    private void masIdsLocal() {
        if (actualEs(TokenType.COMA)) {
            match(TokenType.COMA);
            match(TokenType.ID_MET_VAR);
            masIdsLocal();
        } else {
            // ϵ — no hace nada
        }
    }

    // <InitLocalOpc> ::= = <ExpresionCompuesta> | ϵ
    private void initLocalOpc() {
        if (actualEs(TokenType.OP_ASIGN)) {
            match(TokenType.OP_ASIGN);
            expresionCompuesta();
        } else {
            // ϵ — no hace nada
        }
    }

    // <Return> ::= return <ExpresionOpcional>
    private void sentenciaReturn() {
        match(TokenType.PR_RETURN);
        expresionOpcional();
    }

    // <ExpresionOpcional> ::= <Expresion> | ϵ
    private void expresionOpcional() {
        if (actualEn(PRIMEROS_EXPRESION)) {
            expresion();
        } else {
            // ϵ — no hace nada
        }
    }

    // <If> ::= if ( <Expresion> ) <Sentencia> <ElseOpcional>
    private void sentenciaIf() {
        match(TokenType.PR_IF);
        match(TokenType.PAR_A);
        expresion();
        match(TokenType.PAR_C);
        sentencia();
        elseOpcional();
    }

    // <ElseOpcional> ::= else <Sentencia> | ϵ
    // Conflicto FIRST/FOLLOW deliberado sobre "else" (else colgante): con lookahead
    // "else" se toma siempre la rama else -> el else liga con el if más cercano.
    private void elseOpcional() {
        if (actualEs(TokenType.PR_ELSE)) {
            match(TokenType.PR_ELSE);
            sentencia();
        } else {
            // ϵ — no hace nada (reduce por ϵ; el "else" liga con el if más cercano)
        }
    }

    // <While> ::= while ( <Expresion> ) <Sentencia>
    private void sentenciaWhile() {
        match(TokenType.PR_WHILE);
        match(TokenType.PAR_A);
        expresion();
        match(TokenType.PAR_C);
        sentencia();
    }

    // <For> ::= for ( <ClausulasFor> ) <Sentencia>
    // El cuerpo es <Sentencia> (no <Bloque>), igual que <If>/<While>: admite
    // tanto una sentencia simple sin llaves como un bloque.
    private void sentenciaFor() {
        match(TokenType.PR_FOR);
        match(TokenType.PAR_A);
        clausulasFor();
        match(TokenType.PAR_C);
        sentencia();
    }

    // <ClausulasFor> ::= ; <CondFor> ; <ActFor>
    //                 |  <TipoPrimitivo> idMetVar <TrasIdForTipo>
    //                 |  idGen idMetVar <TrasIdForTipo>
    //                 |  idClase <TrasIdClaseFor>
    //                 |  <Expresion> ; <CondFor> ; <ActFor>
    // idClase se intercepta antes que el resto de <Expresion> por la misma
    // razón que en sentencia()/<SentIdClase>: tras "idClase" puede seguir un
    // tipo (declaración clásica o for-each) o el resto de una llamada estática
    // (expresión de inicialización, p. ej. Fabrica.reset()); un token más lo
    // decide en trasIdClaseFor().
    private void clausulasFor() {
        if (actualEs(TokenType.PUNTO_COMA)) {
            match(TokenType.PUNTO_COMA);
            condFor();
            match(TokenType.PUNTO_COMA);
            actFor();
        } else if (actualEn(PRIMEROS_TIPO_PRIMITIVO)) {
            tipoPrimitivo();
            match(TokenType.ID_MET_VAR);
            trasIdForTipo();
        } else if (actualEs(TokenType.ID_GEN)) {
            match(TokenType.ID_GEN);
            match(TokenType.ID_MET_VAR);
            trasIdForTipo();
        } else if (actualEs(TokenType.ID_CLASE)) {
            match(TokenType.ID_CLASE);
            trasIdClaseFor();
        } else if (actualEn(PRIMEROS_EXPRESION)) {
            expresion();
            match(TokenType.PUNTO_COMA);
            condFor();
            match(TokenType.PUNTO_COMA);
            actFor();
        } else {
            error("\";\", un tipo o una expresión (inicialización del \"for\")");
        }
    }

    // <TrasIdForTipo> ::= : <Expresion>                          (for-each)
    //                  |  <InitLocalOpc> ; <CondFor> ; <ActFor>  (for clásico con declaración)
    private void trasIdForTipo() {
        if (actualEs(TokenType.DOS_PUNTOS)) {
            match(TokenType.DOS_PUNTOS);
            expresion();
        } else {
            initLocalOpc();
            match(TokenType.PUNTO_COMA);
            condFor();
            match(TokenType.PUNTO_COMA);
            actFor();
        }
    }

    // <TrasIdClaseFor> ::= <TipoGenericoOpcional> idMetVar <TrasIdForTipo>
    //                   |  . idMetVar <ArgsActuales> <ReferenciaResto> <ExpresionCompuestaResto> <RestoAsignacion> ; <CondFor> ; <ActFor>
    // "." => era una llamada estática como expresión de inicialización; misma
    // reconstrucción de cola de <Expresion> que usa <SentIdClase>. Cualquier
    // otra cosa => era un tipo clase (declaración o for-each).
    private void trasIdClaseFor() {
        if (actualEs(TokenType.PUNTO)) {
            match(TokenType.PUNTO);
            match(TokenType.ID_MET_VAR);
            argsActuales();
            referenciaResto();
            expresionCompuestaResto();
            restoAsignacion();
            match(TokenType.PUNTO_COMA);
            condFor();
            match(TokenType.PUNTO_COMA);
            actFor();
        } else {
            tipoGenericoOpcional();
            match(TokenType.ID_MET_VAR);
            trasIdForTipo();
        }
    }

    // <CondFor> ::= <Expresion> | ϵ
    private void condFor() {
        if (actualEn(PRIMEROS_EXPRESION)) {
            expresion();
        } else {
            // ϵ — no hace nada (cláusula vacía: for (init;;act))
        }
    }

    // <ActFor> ::= <Expresion> | ϵ
    private void actFor() {
        if (actualEn(PRIMEROS_EXPRESION)) {
            expresion();
        } else {
            // ϵ — no hace nada (cláusula vacía: for (init;cond;))
        }
    }

    // <Expresion> ::= <ExpresionCompuesta> <RestoAsignacion>
    private void expresion() {
        expresionCompuesta();
        restoAsignacion();
    }

    // <RestoAsignacion> ::= <OperadorAsignacion> <ExpresionCompuesta> | ϵ
    private void restoAsignacion() {
        if (actualEs(TokenType.OP_ASIGN)) {
            operadorAsignacion();
            expresionCompuesta();
        } else {
            // ϵ — no hace nada
        }
    }

    // <OperadorAsignacion> ::= =
    private void operadorAsignacion() {
        match(TokenType.OP_ASIGN);
    }

    // <ExpresionCompuesta> ::= <ExpresionBasica> <ExpresionCompuestaResto>
    private void expresionCompuesta() {
        expresionBasica();
        expresionCompuestaResto();
        ternarioOpcional();
    }

    // <ExpresionCompuestaResto> ::= <OperadorBinario> <ExpresionBasica> <ExpresionCompuestaResto> | ϵ
    private void expresionCompuestaResto() {
        if (actualEn(PRIMEROS_OP_BINARIO)) {
            operadorBinario();
            expresionBasica();
            expresionCompuestaResto();
        } else {
            // ϵ — no hace nada
        }
    }

    // <OperadorBinario> ::= || | && | == | != | < | > | <= | >= | + | - | * | / | %
    private void operadorBinario() {
        if (actualEn(PRIMEROS_OP_BINARIO)) {
            avanzar();
        } else {
            error("un operador binario");
        }
    }

    // <TernarioOpcional> ::= ? <ExpresionCompuesta> : <ExpresionCompuesta> | ϵ
    // REQ-AS-013. Se engancha en <ExpresionCompuesta> (no en <ExpresionBasica>) para que la
    // condición sea la cadena binaria completa que ya se armó en expresionBasica()+expresionCompuestaResto(),
    // dejando al ternario con menos precedencia que cualquier operador binario (como en Java).
    // El <valorFalse> es <ExpresionCompuesta>, que ya termina en su propio ternarioOpcional(): un
    // ternario anidado ahí (a ? b : c ? d : e) se resuelve solo, sin repetir la llamada acá, y da
    // asociatividad a derecha (a ? b : (c ? d : e)).
    private void ternarioOpcional() {
        if (actualEs(TokenType.INTERROGACION)) {
            match(TokenType.INTERROGACION);
            expresionCompuesta();
            match(TokenType.DOS_PUNTOS);
            expresionCompuesta();
        } else {
            // ϵ — no hace nada
        }
    }

    // <ExpresionBasica> ::= <OperadorUnario> <Operando> <PostfijoOpcional> | <Operando> <PostfijoOpcional>
    private void expresionBasica() {
        if (actualEn(PRIMEROS_OP_UNARIO)) {
            operadorUnario();
            operando();
            postfijoOpcional();
        } else if (actualEn(PRIMEROS_OPERANDO)) {
            operando();
            postfijoOpcional();
        } else {
            error("una expresión");
        }
    }

    // <PostfijoOpcional> ::= ++ <PostfijoOpcional> | -- <PostfijoOpcional> | ϵ
    // REQ-AS-014. Va pegado a <Operando> dentro de <ExpresionBasica> (no como sufijo de
    // <ExpresionCompuesta>) para que se aplique a CADA término de la cadena binaria, no sólo al
    // primero: expresionBasica() se llama una vez por término (acá y desde
    // expresionCompuestaResto()), así que "a + x++" también queda cubierto sin duplicar el chequeo.
    // Es recursiva (no un solo ++/--) para admitir encadenados como "a++--": la gramática real de
    // Java también es recursiva ahí (PostIncrementExpression/PostDecrementExpression toman como
    // operando otro PostfixExpression) y sólo lo rechaza en el chequeo de tipos ("a++" da un valor,
    // no una variable) — acá, igual que con "1++" o "1 = 2;", esa distinción queda para semántica.
    private void postfijoOpcional() {
        if (actualEs(TokenType.OP_INCREMENTO) || actualEs(TokenType.OP_DECREMENTO)) {
            avanzar();
            postfijoOpcional();
        } else {
            // ϵ — no hace nada
        }
    }

    // <OperadorUnario> ::= + | - | !
    private void operadorUnario() {
        if (actualEn(PRIMEROS_OP_UNARIO)) {
            avanzar();
        } else {
            error("\"+\", \"-\" o \"!\"");
        }
    }

    // <Operando> ::= <Primitivo>
    //            |  this <ReferenciaResto>
    //            |  stringLiteral <ReferenciaResto>
    //            |  new <RestoNew> <ReferenciaResto>
    //            |  <LlamadaMetodoEstatico> <ReferenciaResto>
    //            |  idMetVar <TrasId>
    //            |  ( <TrasParen>
    // Forma factorizada de "Gramática Expandida (Logros)": inlinea <Referencia>,
    // <Primario> y <ExpresionParentizada> para poder repartir los prefijos "("
    // (expresión parentizada vs. lambda) e "idMetVar" (variable vs. lambda de 1
    // parámetro sin paréntesis). Decisión con un solo token; ARROW no está en
    // ningún FIRST, es siempre marcador infijo de lambda.
    private void operando() {
        if (actualEn(PRIMEROS_PRIMITIVO)) {
            primitivo();
        } else if (actualEs(TokenType.PR_THIS)) {
            match(TokenType.PR_THIS);
            referenciaResto();
        } else if (actualEs(TokenType.LIT_STRING)) {
            match(TokenType.LIT_STRING);
            referenciaResto();
        } else if (actualEs(TokenType.PR_NEW)) {
            match(TokenType.PR_NEW);
            restoNew();
            referenciaResto();
        } else if (actualEs(TokenType.ID_CLASE)) {
            llamadaMetodoEstatico();
            referenciaResto();
        } else if (actualEs(TokenType.ID_MET_VAR)) {
            match(TokenType.ID_MET_VAR);
            trasId();
        } else if (actualEs(TokenType.PAR_A)) {
            match(TokenType.PAR_A);
            trasParen();
        } else {
            error("un operando");
        }
    }

    // <TrasId> ::= -> <Expresion>                    (lambda de 1 parámetro: x -> e)
    //          |  <ArgsActualesOpcional> <ReferenciaResto>   (variable o llamada: x | x(a) | x.f ...)
    private void trasId() {
        if (actualEs(TokenType.ARROW)) {
            match(TokenType.ARROW);
            expresion();
        } else {
            argsActualesOpcional();
            referenciaResto();
        }
    }

    // <TrasParen> ::= ) -> <Expresion>              (lambda de 0 parámetros: () -> e)
    //             |  idMetVar <TrasParenId>         (posible parámetro: hay que ver qué sigue)
    //             |  <Expresion> ) <ReferenciaResto> (cualquier otro arranque: nunca es lambda,
    //                                                  un parámetro siempre es "idMetVar" o "()")
    private void trasParen() {
        if (actualEs(TokenType.PAR_C)) {
            match(TokenType.PAR_C);
            match(TokenType.ARROW);
            expresion();
        } else if (actualEs(TokenType.ID_MET_VAR)) {
            match(TokenType.ID_MET_VAR);
            trasParenId();
        } else if (actualEn(PRIMEROS_EXPRESION)) {
            expresion();
            match(TokenType.PAR_C);
            referenciaResto();
        } else {
            error("\")\" (lambda sin parámetros) o una expresión");
        }
    }

    // <TrasParenId> ::= , <ListaIdLambda> ) -> <Expresion>   (lambda de >=2 parámetros)
    //               |  ) <DecidirTrasCierre>                  (un solo idMetVar: caso ambiguo real)
    //               |  <TrasId> <ExpresionCompuestaResto> <RestoAsignacion> ) <ReferenciaResto>
    //                  (el idMetVar era el comienzo de una expresión más grande —operador, ".",
    //                  "[", "(", "=", o incluso un "->" de lambda anidada vía <TrasId>—: se sigue
    //                  como expresión normal y, al cerrar, ya no se vuelve a ofrecer "->").
    // Reusa <TrasId>/<ExpresionCompuestaResto>/<RestoAsignacion>/<ReferenciaResto> para no
    // duplicar la jerarquía de precedencia de <Expresion>.
    private void trasParenId() {
        if (actualEs(TokenType.COMA)) {
            match(TokenType.COMA);
            listaIdLambda();
            match(TokenType.PAR_C);
            match(TokenType.ARROW);
            expresion();
        } else if (actualEs(TokenType.PAR_C)) {
            match(TokenType.PAR_C);
            decidirTrasCierre();
        } else {
            trasId();
            postfijoOpcional();
            expresionCompuestaResto();
            ternarioOpcional();
            restoAsignacion();
            match(TokenType.PAR_C);
            referenciaResto();
        }
    }

    // <DecidirTrasCierre> ::= -> <Expresion>        (era ( x ) -> e : lambda de 1 parámetro)
    //                      |  <ReferenciaResto>      (era ( x ) : expresión parentizada)
    private void decidirTrasCierre() {
        if (actualEs(TokenType.ARROW)) {
            match(TokenType.ARROW);
            expresion();
        } else {
            referenciaResto();
        }
    }

    // <ListaIdLambda> ::= idMetVar <RestoListaIdLambda>
    private void listaIdLambda() {
        match(TokenType.ID_MET_VAR);
        restoListaIdLambda();
    }

    // <RestoListaIdLambda> ::= , idMetVar <RestoListaIdLambda> | ϵ
    private void restoListaIdLambda() {
        if (actualEs(TokenType.COMA)) {
            match(TokenType.COMA);
            match(TokenType.ID_MET_VAR);
            restoListaIdLambda();
        } else {
            // ϵ — no hace nada
        }
    }

    // <Primitivo> ::= true | false | intLiteral | charLiteral | null
    private void primitivo() {
        if (actualEn(PRIMEROS_PRIMITIVO)) {
            avanzar();
        } else {
            error("un literal (true, false, intLiteral, charLiteral, null)");
        }
    }

    // <ReferenciaResto> ::= . idMetVar <ArgsActualesOpcional> <ReferenciaResto>
    //                    |  [ <Expresion> ] <ReferenciaResto>
    //                    |  ϵ
    private void referenciaResto() {
        if (actualEs(TokenType.PUNTO)) {
            match(TokenType.PUNTO);
            match(TokenType.ID_MET_VAR);
            argsActualesOpcional();
            referenciaResto();
        } else if (actualEs(TokenType.CORCHETE_A)) {
            match(TokenType.CORCHETE_A);
            expresion();
            match(TokenType.CORCHETE_C);
            referenciaResto();
        } else {
            // ϵ — no hace nada
        }
    }

    // <ArgsActualesOpcional> ::= <ArgsActuales> | ϵ
    private void argsActualesOpcional() {
        if (actualEs(TokenType.PAR_A)) {
            argsActuales();
        } else {
            // ϵ — no hace nada
        }
    }

    // <RestoNew> ::= <TipoPrimitivo> <DimensionesNew>
    //            |  idGen <DimensionesNew>
    //            |  idClase <TipoGenericoOpcionalNew> <RestoNewIdClase>
    private void restoNew() {
        if (actualEn(PRIMEROS_TIPO_PRIMITIVO)) {
            tipoPrimitivo();
            dimensionesNew();
        } else if (actualEs(TokenType.ID_GEN)) {
            match(TokenType.ID_GEN);
            dimensionesNew();
        } else if (actualEs(TokenType.ID_CLASE)) {
            match(TokenType.ID_CLASE);
            tipoGenericoOpcionalNew();
            restoNewIdClase();
        } else {
            error("un tipo después de \"new\"");
        }
    }

    // <RestoNewIdClase> ::= <DimensionesNew> | <ArgsActuales>
    private void restoNewIdClase() {
        if (actualEs(TokenType.CORCHETE_A)) {
            dimensionesNew();
        } else if (actualEs(TokenType.PAR_A)) {
            argsActuales();
        } else {
            error("\"[\" (arreglo) o \"(\" (constructor)");
        }
    }

    // <LlamadaMetodoEstatico> ::= idClase . idMetVar <ArgsActuales>
    private void llamadaMetodoEstatico() {
        match(TokenType.ID_CLASE);
        match(TokenType.PUNTO);
        match(TokenType.ID_MET_VAR);
        argsActuales();
    }

    // <DimensionesNew> ::= [ <TrasCorcheteNew>
    private void dimensionesNew() {
        match(TokenType.CORCHETE_A);
        trasCorcheteNew();
    }

    // <TrasCorcheteNew> ::= ] <MasCorchetesVaciosNew> <InicializadorArreglo>
    //                   |  <Expresion> ] <DimensionesConTamanioOpc>
    // "]" inmediato (REQ-AS-012): todos los corchetes de esta creación van
    // vacíos y viene un inicializador obligatorio ({1,2,3}) — no se puede
    // mezclar tamaño e inicializador, igual que en Java. Cualquier otra cosa:
    // sigue la forma de tamaño de siempre, sin cambios.
    private void trasCorcheteNew() {
        if (actualEs(TokenType.CORCHETE_C)) {
            match(TokenType.CORCHETE_C);
            masCorchetesVaciosNew();
            inicializadorArreglo();
        } else {
            expresion();
            match(TokenType.CORCHETE_C);
            dimensionesConTamanioOpc();
        }
    }

    // <MasCorchetesVaciosNew> ::= [ ] <MasCorchetesVaciosNew> | ϵ
    private void masCorchetesVaciosNew() {
        if (actualEs(TokenType.CORCHETE_A)) {
            match(TokenType.CORCHETE_A);
            match(TokenType.CORCHETE_C);
            masCorchetesVaciosNew();
        } else {
            // ϵ — no hace nada
        }
    }

    // <InicializadorArreglo> ::= { <ListaValoresArregloOpcional> }
    private void inicializadorArreglo() {
        match(TokenType.LLAVE_A);
        listaValoresArregloOpcional();
        match(TokenType.LLAVE_C);
    }

    // <ListaValoresArregloOpcional> ::= <ListaValoresArreglo> | ϵ
    private void listaValoresArregloOpcional() {
        if (actualEs(TokenType.LLAVE_A) || actualEn(PRIMEROS_EXPRESION)) {
            listaValoresArreglo();
        } else {
            // ϵ — no hace nada ("{ }" vacío también es válido)
        }
    }

    // <ListaValoresArreglo> ::= <ValorArreglo> <RestoValoresArreglo>
    private void listaValoresArreglo() {
        valorArreglo();
        restoValoresArreglo();
    }

    // <RestoValoresArreglo> ::= , <ValorArreglo> <RestoValoresArreglo> | ϵ
    private void restoValoresArreglo() {
        if (actualEs(TokenType.COMA)) {
            match(TokenType.COMA);
            valorArreglo();
            restoValoresArreglo();
        } else {
            // ϵ — no hace nada
        }
    }

    // <ValorArreglo> ::= <ExpresionCompuesta> | <InicializadorArreglo>
    // Anidado habilita arreglos multidimensionales con inicializador
    // (new int[][]{{1,2},{3,4}}) sin costo de factorización adicional.
    private void valorArreglo() {
        if (actualEs(TokenType.LLAVE_A)) {
            inicializadorArreglo();
        } else if (actualEn(PRIMEROS_EXPRESION)) {
            expresionCompuesta();
        } else {
            error("un valor (expresión o \"{ ... }\" anidado)");
        }
    }

    // <DimensionesConTamanioOpc> ::= [ <Expresion> ] <DimensionesConTamanioOpc> | ϵ
    // Conflicto FIRST/FOLLOW deliberado sobre "[": con lookahead "[" se siguen
    // consumiendo dimensiones (new int[2][3] = arreglo 2D, no (new int[2])[3]).
    // El "[" de acceso a arreglo recién se toma en <ReferenciaResto>.
    private void dimensionesConTamanioOpc() {
        if (actualEs(TokenType.CORCHETE_A)) {
            match(TokenType.CORCHETE_A);
            expresion();
            match(TokenType.CORCHETE_C);
            dimensionesConTamanioOpc();
        } else {
            // ϵ — no hace nada (reduce por ϵ; el "[" restante lo toma <ReferenciaResto>)
        }
    }

    // <ArgsActuales> ::= ( <ListaExpsOpcional> )
    private void argsActuales() {
        match(TokenType.PAR_A);
        listaExpsOpcional();
        match(TokenType.PAR_C);
    }

    // <ListaExpsOpcional> ::= <ListaExps> | ϵ
    private void listaExpsOpcional() {
        if (actualEn(PRIMEROS_EXPRESION)) {
            listaExps();
        } else {
            // ϵ — no hace nada
        }
    }

    // <ListaExps> ::= <Expresion> <RestoListaExps>
    private void listaExps() {
        expresion();
        restoListaExps();
    }

    // <RestoListaExps> ::= , <Expresion> <RestoListaExps> | ϵ
    private void restoListaExps() {
        if (actualEs(TokenType.COMA)) {
            match(TokenType.COMA);
            expresion();
            restoListaExps();
        } else {
            // ϵ — no hace nada
        }
    }
}
