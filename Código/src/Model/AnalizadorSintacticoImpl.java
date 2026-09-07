package Model;

import java.util.EnumSet;
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
 * <p>Pendientes (ver el documento de diseño):
 * <ul>
 *   <li>Extensiones {@code REQ-AS-005..014} (lambdas, {@code for}, visibilidad,
 *       {@code var} clásico, genéricos anidados y diamante, inicializadores,
 *       ternario, {@code ++}/{@code --}): todavía no están en la gramática ni acá.</li>
 *   <li>{@code REQ-AS-008}: recuperación en modo pánico. Hoy {@link #error} lanza
 *       {@link ErrorSintactico} y se corta en el primer error.</li>
 *   <li>Reporte vía listener (como {@code ResultadoLexicoListener}) en vez de
 *       excepción, para no acoplar el analizador a quien consume los errores.</li>
 *   <li>Dos conflictos se resuelven por convención en el método, no en la
 *       gramática: {@code else} colgante ({@link #elseOpcional}) y {@code [} tras
 *       dimensiones ({@link #dimensionesConTamanioOpc}).</li>
 * </ul>
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

    // FIRST(<Miembro>) = { public static void } ∪ FIRST(<Tipo>)
    private static final Set<TokenType> PRIMEROS_MIEMBRO = EnumSet.of(
            TokenType.PR_PUBLIC, TokenType.PR_STATIC, TokenType.PR_VOID,
            TokenType.PR_BOOLEAN, TokenType.PR_CHAR, TokenType.PR_INT,
            TokenType.ID_CLASE, TokenType.ID_GEN);

    private static final Set<TokenType> PRIMEROS_PRIMITIVO = EnumSet.of(
            TokenType.PR_TRUE, TokenType.PR_FALSE, TokenType.LIT_INT,
            TokenType.LIT_CHAR, TokenType.PR_NULL);

    // FIRST(<Primario>) = FIRST(<Referencia>)
    private static final Set<TokenType> PRIMEROS_PRIMARIO = EnumSet.of(
            TokenType.PR_THIS, TokenType.LIT_STRING, TokenType.ID_MET_VAR,
            TokenType.PR_NEW, TokenType.ID_CLASE, TokenType.PAR_A);

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

    // FIRST(<Sentencia>) = { ; var return if while { } ∪ FIRST(<Expresion>)
    private static final Set<TokenType> PRIMEROS_SENTENCIA = EnumSet.of(
            TokenType.PUNTO_COMA, TokenType.PR_VAR, TokenType.PR_RETURN,
            TokenType.PR_IF, TokenType.PR_WHILE, TokenType.LLAVE_A,
            TokenType.OP_MAS, TokenType.OP_MENOS, TokenType.OP_NOT,
            TokenType.PR_TRUE, TokenType.PR_FALSE, TokenType.LIT_INT,
            TokenType.LIT_CHAR, TokenType.PR_NULL,
            TokenType.PR_THIS, TokenType.LIT_STRING, TokenType.ID_MET_VAR,
            TokenType.PR_NEW, TokenType.ID_CLASE, TokenType.PAR_A);

    // ------------------------------------------------------------------
    // Infraestructura: start / match / lookahead / error
    // ------------------------------------------------------------------

    @Override
    public void start() {
        tokenActual = lexico.nextToken();
        inicial();
        // REQ-AS-004: <Inicial> ya consume el eof con su match(EOF) final.
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

    private void error(String esperado) {
        String encontrado = tokenActual.getTipo().getNombre()
                + " (\"" + tokenActual.getLexema() + "\")";
        throw new ErrorSintactico(tokenActual.getLinea(), encontrado, esperado);
        // TODO REQ-AS-008: reportar y sincronizar (modo pánico) en vez de abortar.
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
        }
        // ϵ  (FOLLOW = { eof })
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
        }
    }

    // <ExtensionOpcional> ::= extends <TipoReferencia> | ϵ
    private void extensionOpcional() {
        if (actualEs(TokenType.PR_EXTENDS)) {
            match(TokenType.PR_EXTENDS);
            tipoReferencia();
        }
    }

    // <ListaMiembros> ::= <Miembro> <ListaMiembros> | ϵ
    private void listaMiembros() {
        if (actualEn(PRIMEROS_MIEMBRO)) {
            miembro();
            listaMiembros();
        }
    }

    // <ListaMetodosInterfaz> ::= <MetodoInterfaz> <ListaMetodosInterfaz> | ϵ
    private void listaMetodosInterfaz() {
        if (actualEn(PRIMEROS_TIPO_METODO)) {
            metodoInterfaz();
            listaMetodosInterfaz();
        }
    }

    // <Miembro> ::= public idClase <ArgsFormales> <Bloque>
    //            |  static <TipoMetodo> idMetVar <ArgsFormales> <Bloque>
    //            |  void idMetVar <ArgsFormales> <Bloque>
    //            |  <Tipo> idMetVar <RestoMiembro>
    private void miembro() {
        if (actualEs(TokenType.PR_PUBLIC)) {
            match(TokenType.PR_PUBLIC);
            match(TokenType.ID_CLASE);
            argsFormales();
            bloque();
        } else if (actualEs(TokenType.PR_STATIC)) {
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
        } else if (actualEn(PRIMEROS_TIPO)) {
            tipo();
            match(TokenType.ID_MET_VAR);
            restoMiembro();
        } else {
            error("un miembro de clase (\"public\", \"static\", \"void\" o un tipo)");
        }
    }

    // <RestoMiembro> ::= ; | <ArgsFormales> <Bloque>
    private void restoMiembro() {
        if (actualEs(TokenType.PUNTO_COMA)) {
            match(TokenType.PUNTO_COMA);
        } else if (actualEs(TokenType.PAR_A)) {
            argsFormales();
            bloque();
        } else {
            error("\";\" (atributo) o \"(\" (método)");
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
        }
    }

    // <InstanciadoOParametrico> ::= idGen | idClase
    private void instanciadoOParametrico() {
        if (actualEs(TokenType.ID_GEN)) {
            match(TokenType.ID_GEN);
        } else if (actualEs(TokenType.ID_CLASE)) {
            match(TokenType.ID_CLASE);
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
        }
    }

    // <Sentencia> ::= ;
    //             |  <Expresion> ;
    //             |  <VarLocal> ;
    //             |  <Return> ;
    //             |  <If>
    //             |  <While>
    //             |  <Bloque>
    private void sentencia() {
        if (actualEs(TokenType.PUNTO_COMA)) {
            match(TokenType.PUNTO_COMA);
        } else if (actualEs(TokenType.PR_VAR)) {
            varLocal();
            match(TokenType.PUNTO_COMA);
        } else if (actualEs(TokenType.PR_RETURN)) {
            sentenciaReturn();
            match(TokenType.PUNTO_COMA);
        } else if (actualEs(TokenType.PR_IF)) {
            sentenciaIf();
        } else if (actualEs(TokenType.PR_WHILE)) {
            sentenciaWhile();
        } else if (actualEs(TokenType.LLAVE_A)) {
            bloque();
        } else if (actualEn(PRIMEROS_EXPRESION)) {
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

    // <Return> ::= return <ExpresionOpcional>
    private void sentenciaReturn() {
        match(TokenType.PR_RETURN);
        expresionOpcional();
    }

    // <ExpresionOpcional> ::= <Expresion> | ϵ
    private void expresionOpcional() {
        if (actualEn(PRIMEROS_EXPRESION)) {
            expresion();
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
    }

    // <ExpresionCompuestaResto> ::= <OperadorBinario> <ExpresionBasica> <ExpresionCompuestaResto> | ϵ
    private void expresionCompuestaResto() {
        if (actualEn(PRIMEROS_OP_BINARIO)) {
            operadorBinario();
            expresionBasica();
            expresionCompuestaResto();
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

    // <ExpresionBasica> ::= <OperadorUnario> <Operando> | <Operando>
    private void expresionBasica() {
        if (actualEn(PRIMEROS_OP_UNARIO)) {
            operadorUnario();
            operando();
        } else if (actualEn(PRIMEROS_OPERANDO)) {
            operando();
        } else {
            error("una expresión");
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

    // <Operando> ::= <Primitivo> | <Referencia>
    private void operando() {
        if (actualEn(PRIMEROS_PRIMITIVO)) {
            primitivo();
        } else if (actualEn(PRIMEROS_PRIMARIO)) {
            referencia();
        } else {
            error("un operando");
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

    // <Referencia> ::= <Primario> <ReferenciaResto>
    private void referencia() {
        primario();
        referenciaResto();
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
        }
    }

    // <ArgsActualesOpcional> ::= <ArgsActuales> | ϵ
    private void argsActualesOpcional() {
        if (actualEs(TokenType.PAR_A)) {
            argsActuales();
        }
    }

    // <Primario> ::= this
    //            |  stringLiteral
    //            |  idMetVar <ArgsActualesOpcional>
    //            |  new <RestoNew>
    //            |  <LlamadaMetodoEstatico>
    //            |  <ExpresionParentizada>
    private void primario() {
        if (actualEs(TokenType.PR_THIS)) {
            match(TokenType.PR_THIS);
        } else if (actualEs(TokenType.LIT_STRING)) {
            match(TokenType.LIT_STRING);
        } else if (actualEs(TokenType.ID_MET_VAR)) {
            match(TokenType.ID_MET_VAR);
            argsActualesOpcional();
        } else if (actualEs(TokenType.PR_NEW)) {
            match(TokenType.PR_NEW);
            restoNew();
        } else if (actualEs(TokenType.ID_CLASE)) {
            llamadaMetodoEstatico();
        } else if (actualEs(TokenType.PAR_A)) {
            expresionParentizada();
        } else {
            error("un primario (this, string, idMetVar, new, idClase o \"(\")");
        }
    }

    // <RestoNew> ::= <TipoPrimitivo> <DimensionesConTamanio>
    //            |  idGen <DimensionesConTamanio>
    //            |  idClase <TipoGenericoOpcional> <RestoNewIdClase>
    private void restoNew() {
        if (actualEn(PRIMEROS_TIPO_PRIMITIVO)) {
            tipoPrimitivo();
            dimensionesConTamanio();
        } else if (actualEs(TokenType.ID_GEN)) {
            match(TokenType.ID_GEN);
            dimensionesConTamanio();
        } else if (actualEs(TokenType.ID_CLASE)) {
            match(TokenType.ID_CLASE);
            tipoGenericoOpcional();
            restoNewIdClase();
        } else {
            error("un tipo después de \"new\"");
        }
    }

    // <RestoNewIdClase> ::= <DimensionesConTamanio> | <ArgsActuales>
    private void restoNewIdClase() {
        if (actualEs(TokenType.CORCHETE_A)) {
            dimensionesConTamanio();
        } else if (actualEs(TokenType.PAR_A)) {
            argsActuales();
        } else {
            error("\"[\" (arreglo) o \"(\" (constructor)");
        }
    }

    // <ExpresionParentizada> ::= ( <Expresion> )
    private void expresionParentizada() {
        match(TokenType.PAR_A);
        expresion();
        match(TokenType.PAR_C);
    }

    // <LlamadaMetodoEstatico> ::= idClase . idMetVar <ArgsActuales>
    private void llamadaMetodoEstatico() {
        match(TokenType.ID_CLASE);
        match(TokenType.PUNTO);
        match(TokenType.ID_MET_VAR);
        argsActuales();
    }

    // <DimensionesConTamanio> ::= [ <Expresion> ] <DimensionesConTamanioOpc>
    private void dimensionesConTamanio() {
        match(TokenType.CORCHETE_A);
        expresion();
        match(TokenType.CORCHETE_C);
        dimensionesConTamanioOpc();
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
        }
    }
}
