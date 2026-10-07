package Model.sintactico;

import Model.lexico.AnalizadorLexico;
import Model.lexico.Token;
import Model.lexico.TokenType;
import Model.semantico.Atributo;
import Model.semantico.Clase;
import Model.semantico.Constructor;
import Model.semantico.Interfaz;
import Model.semantico.Metodo;
import Model.semantico.Parametro;
import Model.semantico.TablaSimbolos;
import Model.semantico.Tipo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;


public class AnalizadorSintacticoImpl implements AnalizadorSintactico {

    private final AnalizadorLexico lexico;

    /** Único token de lookahead: el próximo sin consumir. */
    private Token tokenActual;

    private final TablaSimbolos tablaSimbolo;

    public AnalizadorSintacticoImpl(AnalizadorLexico lex, TablaSimbolos tablaSimbolo) {
        this.lexico = lex;
        this.tablaSimbolo = tablaSimbolo;
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

    /**
     * true justo después de que {@link #sincronizar()} consumió un ";" como
     * parte de recuperarse de un error ya reportado. Le avisa al
     * {@code match(PUNTO_COMA)} de cierre de la producción en curso que ese
     * delimitador ya fue consumido, para que no vuelva a exigirlo -- si lo
     * hiciera, generaría un eco del mismo error contra el primer token de lo
     * que venga después (posiblemente código válido). Cualquier avance real
     * de token la limpia: solo protege el primer match(PUNTO_COMA) que se
     * encuentre inmediatamente después.
     */
    private boolean delimitadorConsumidoPorRecuperacion = false;

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

    /**
     * Cierra una producción con su ";" final (RestoDeclLocal, Sentencia,
     * RestoMiembro, etc.). Si el error() de un sub-parseo inmediatamente
     * anterior ya sincronizó consumiendo exactamente ese ";" -- porque el
     * token ofensivo YA era el delimitador que esta misma producción iba a
     * pedir -- no lo vuelve a exigir: ya fue consumido, y volver a exigirlo
     * generaría un eco del mismo error contra el primer token de lo que
     * venga después (posiblemente código perfectamente válido).
     */
    private void matchCierre(TokenType esperado) {
        if (delimitadorConsumidoPorRecuperacion) {
            delimitadorConsumidoPorRecuperacion = false;
            return;
        }
        match(esperado);
    }

    /** Pide el próximo token al léxico. Al llegar a EOF sigue devolviendo EOF. */
    private void avanzar() {
        delimitadorConsumidoPorRecuperacion = false;
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

    private void sincronizar() {
        if (!actualEn(TOKENS_SINCRONIZACION)) {
            avanzar();
            while (!actualEn(TOKENS_SINCRONIZACION)) {
                avanzar();
            }
        }
        if (actualEs(TokenType.PUNTO_COMA)) {
            avanzar();
            delimitadorConsumidoPorRecuperacion = true;
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
    // Pasada 1 del EDT (ver "Acciones semánticas sobre la gramática original"
    // en analizador_semantico.md): apenas se reconoce el nombre, se crea la
    // Clase (vacía) y se registra en la tabla. Herencia/miembros quedan para
    // la Pasada 2 (fuera de alcance de este pase).
    private void clase() {
        match(TokenType.PR_CLASS);
        Token nombreClase = tokenActual;
        match(TokenType.ID_CLASE);
        Clase claseActual = crearClase(nombreClase);
        tablaSimbolo.insertarClase(claseActual);
        genericidadOpcional();
        herenciaOpcional();
        match(TokenType.LLAVE_A);
        listaMiembros();
        match(TokenType.LLAVE_C);
    }

    // <Interfaz> ::= interface idClase <GenericidadOpcional> <ExtensionOpcional> { <ListaMetodosInterfaz> }
    // Mismo mecanismo de Pasada 1 que <Clase>: registrar el nombre apenas se
    // conoce, comparte el mismo espacio de nombres que las clases.
    private void interfaz() {
        match(TokenType.PR_INTERFACE);
        Token nombreInterfaz = tokenActual;
        match(TokenType.ID_CLASE);
        Interfaz interfazActual = new Interfaz(nombreInterfaz, tablaSimbolo);
        tablaSimbolo.insertarClase(interfazActual);
        genericidadOpcional();
        extensionOpcional(interfazActual);
        match(TokenType.LLAVE_A);
        listaMetodosInterfaz();
        match(TokenType.LLAVE_C);
    }

    // Construye la Clase y la deja como "clase actual" de la tabla (Estrategia A,
    // ver "Tabla de símbolos: por qué no usar Singleton" en analizador_semantico.md).
    // La inserción en la tabla queda a cargo de clase(), que es quien decide cómo
    // reportar un nombre repetido.
    private Clase crearClase(Token nombre) {
        Clase clase = new Clase(nombre, tablaSimbolo);
        tablaSimbolo.setClaseActual(clase);
        return clase;
    }

    // Construye el Metodo con placeholders (tipoRetorno=null, sin parámetros) y
    // lo agrega a la clase actual. tipoMetodo()/argsFormales() todavía son
    // puramente sintácticos (no resuelven Tipo/Parametro reales) — eso es
    // Pasada 2, fuera de alcance de este pase; tipoRetorno/params se van a
    // tener que pisar con los valores reales cuando se implemente.
    private Metodo crearMetodo(Token nombre, boolean estatico) {
        Metodo metodo = new Metodo(nombre, null, estatico, new ArrayList<>());
        tablaSimbolo.setMetodoActual(metodo);
        tablaSimbolo.agregarMetodoAClaseActual(metodo);
        return metodo;
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
            Token padre = tokenActual; // idClase del padre, antes de que tipoReferencia() lo consuma
            tipoReferencia();
            tablaSimbolo.getClaseActual().setHerencia(padre);
        } else if (actualEs(TokenType.PR_IMPLEMENTS)) {
            match(TokenType.PR_IMPLEMENTS);
            tipoReferencia();
            //TODO: wirear 'implements' a la clase actual (pendiente: Clase.interfaces
            // espera Interfaz ya resuelta, no Token — es una Pasada 2 distinta a esta).
        } else {
            // ϵ — no hace nada
        }
    }

    // <ExtensionOpcional> ::= extends <TipoReferencia> | ϵ
    private void extensionOpcional(Interfaz interfazActual) {
        if (actualEs(TokenType.PR_EXTENDS)) {
            match(TokenType.PR_EXTENDS);
            Token padre = tokenActual; // idClase del padre, antes de que tipoReferencia() lo consuma
            tipoReferencia();
            interfazActual.setExtendida(padre);
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
            Tipo tipoRetorno = tipoMetodo();
            Token nombreMetodo = tokenActual;
            match(TokenType.ID_MET_VAR);
            Metodo metodo = crearMetodo(nombreMetodo, true);
            List<Parametro> params = argsFormales();
            metodo.setTipoRetorno(tipoRetorno);
            metodo.setParametros(params);
            bloque();
        } else if (actualEs(TokenType.PR_VOID)) {
            match(TokenType.PR_VOID);
            Token nombreMetodo = tokenActual;
            match(TokenType.ID_MET_VAR);
            Metodo metodo = crearMetodo(nombreMetodo, false);
            List<Parametro> params = argsFormales();
            metodo.setParametros(params);
            bloque();
        } else if (actualEn(PRIMEROS_TIPO_PRIMITIVO)) {
            Token tipoTok = tipoPrimitivo();
            dimensionesOpcionales();
            Token nombreMiembro = tokenActual;
            match(TokenType.ID_MET_VAR);
            restoMiembro(nombreMiembro, tipoTok);
        } else if (actualEs(TokenType.ID_GEN)) {
            Token tipoTok = tokenActual;
            match(TokenType.ID_GEN);
            dimensionesOpcionales();
            Token nombreMiembro = tokenActual;
            match(TokenType.ID_MET_VAR);
            restoMiembro(nombreMiembro, tipoTok);
        } else if (actualEs(TokenType.ID_CLASE)) {
            Token idClaseTok = tokenActual;
            match(TokenType.ID_CLASE);
            trasIdClaseMiembro(idClaseTok);
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
    private void trasIdClaseMiembro(Token idClassToken) {
        if (actualEs(TokenType.PAR_A)) {
            // "(" pegado, es el constructor.
            List<Parametro> params = argsFormales();
            bloque();
            Constructor constructor = new Constructor(idClassToken, params);
            tablaSimbolo.getClaseActual().agregarConstructor(constructor);
        } else {
            tipoGenericoOpcional();
            dimensionesOpcionales();
            Token nombreMiembro = tokenActual;
            match(TokenType.ID_MET_VAR);
            restoMiembro(nombreMiembro, idClassToken);
        }
    }

    // <RestoMiembro> ::= ;
    //                |  <ArgsFormales> <Bloque>
    //                |  <OperadorAsignacion> <ExpresionCompuesta> ;
    // La tercera rama (REQ-AS-011) es un atributo con inicializador
    // (int x = 5;). FIRST disjuntos con las otras dos ({ ; } / { ( } / { = }):
    // no hace falta factorizar nada, "=" ya alcanza para decidir.
    // nombreMiembro/tipoTok llegan por parámetro (heredado) desde
    // cuerpoMiembro()/trasIdClaseMiembro(): son el tipo y el idMetVar ya
    // consumidos antes de saber si es atributo o método — recién acá se
    // construye el objeto (Estrategia B, ver analizador_semantico.md).
    private void restoMiembro(Token nombreMiembro, Token tipoTok) {
        if (actualEs(TokenType.PUNTO_COMA)) {
            match(TokenType.PUNTO_COMA);
            tablaSimbolo.getClaseActual().agregarAtributo(new Atributo(nombreMiembro, tipoTok));
        } else if (actualEs(TokenType.PAR_A)) {
            Metodo metodo = crearMetodo(nombreMiembro, false);
            List<Parametro> params = argsFormales();
            metodo.setParametros(params);
            metodo.setTipoRetorno(new Tipo(tipoTok));
            bloque();
        } else if (actualEs(TokenType.OP_ASIGN)) {
            operadorAsignacion();
            expresionCompuesta();
            matchCierre(TokenType.PUNTO_COMA);
            tablaSimbolo.getClaseActual().agregarAtributo(new Atributo(nombreMiembro, tipoTok));
        } else {
            error("\";\", \"=\" (atributo) o \"(\" (método)");
        }
    }

    // <MetodoInterfaz> ::= <TipoMetodo> idMetVar <ArgsFormales> ;
    private void metodoInterfaz() {
        tipoMetodo();
        match(TokenType.ID_MET_VAR);
        argsFormales();
        matchCierre(TokenType.PUNTO_COMA);
    }

    // <TipoMetodo> ::= <Tipo> | void
    // null representa "void" (ver Metodo.tipoRetorno) -- no es un Tipo de valor.
    private Tipo tipoMetodo() {
        if (actualEs(TokenType.PR_VOID)) {
            match(TokenType.PR_VOID);
            return null;
        } else if (actualEn(PRIMEROS_TIPO)) {
            return new Tipo(tipo());
        } else {
            error("un tipo o \"void\"");
            return null;
        }
    }

    // <Tipo> ::= <TipoBase> <DimensionesOpcionales>
    // DimensionesOpcionales se consume pero no se guarda todavía -- Atributo/
    // Parametro siguen modelando el tipo como Token crudo (ver "Jerarquía
    // Tipo" en analizador_semantico.md, todavía no implementada).
    private Token tipo() {
        Token t = tipoBase();
        dimensionesOpcionales();
        return t;
    }

    // <TipoBase> ::= <TipoPrimitivo> | <TipoReferencia> | idGen
    private Token tipoBase() {
        if (actualEn(PRIMEROS_TIPO_PRIMITIVO)) {
            return tipoPrimitivo();
        } else if (actualEs(TokenType.ID_CLASE)) {
            return tipoReferencia();
        } else if (actualEs(TokenType.ID_GEN)) {
            Token t = tokenActual;
            match(TokenType.ID_GEN);
            return t;
        } else {
            error("un tipo (primitivo, idClase o idGen)");
            return null;
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
    private Token tipoReferencia() {
        Token t = tokenActual;
        match(TokenType.ID_CLASE);
        tipoGenericoOpcional();
        return t;
    }

    // <TipoPrimitivo> ::= boolean | char | int
    private Token tipoPrimitivo() {
        Token t = tokenActual;
        if (actualEs(TokenType.PR_BOOLEAN)) {
            match(TokenType.PR_BOOLEAN);
        } else if (actualEs(TokenType.PR_CHAR)) {
            match(TokenType.PR_CHAR);
        } else if (actualEs(TokenType.PR_INT)) {
            match(TokenType.PR_INT);
        } else {
            error("\"boolean\", \"char\" o \"int\"");
        }
        return t;
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
    private List<Parametro> argsFormales() {
        match(TokenType.PAR_A);
        List<Parametro> parametros = listaArgsFormalesOpcional();
        match(TokenType.PAR_C);
        return parametros;
    }

    // <ListaArgsFormalesOpcional> ::= <ListaArgsFormales> | ϵ
    private List<Parametro> listaArgsFormalesOpcional() {
        if (actualEn(PRIMEROS_TIPO)) {
            return listaArgsFormales();
        }
        return new ArrayList<>();
    }

    // <ListaArgsFormales> ::= <ArgFormal> <ListaArgsFormalesResto>
    // A <ListaArgsFormalesResto> se le eliminó la recursión a izquierda: el
    // acumulador se arrastra a derecha y se cierra en el caso base ϵ (ver "De
    // la gramática original a la LL(1)" en analizador_semantico.md).
    private List<Parametro> listaArgsFormales() {
        Parametro primero = argFormal();
        List<Parametro> resto = listaArgsFormalesResto();
        resto.add(0, primero);
        return resto;
    }

    // <ListaArgsFormalesResto> ::= , <ArgFormal> <ListaArgsFormalesResto> | ϵ
    private List<Parametro> listaArgsFormalesResto() {
        if (actualEs(TokenType.COMA)) {
            match(TokenType.COMA);
            Parametro p = argFormal();
            List<Parametro> resto = listaArgsFormalesResto();
            resto.add(0, p);
            return resto;
        }
        return new ArrayList<>();
    }

    // <ArgFormal> ::= <Tipo> idMetVar
    private Parametro argFormal() {
        Token tipoTok = tipo();
        Token nombre = tokenActual;
        match(TokenType.ID_MET_VAR);
        return new Parametro(nombre, tipoTok);
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
            matchCierre(TokenType.PUNTO_COMA);
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
            matchCierre(TokenType.PUNTO_COMA);
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
            matchCierre(TokenType.PUNTO_COMA);
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
            matchCierre(TokenType.PUNTO_COMA);
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
        matchCierre(TokenType.PUNTO_COMA);
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
            matchCierre(TokenType.PUNTO_COMA);
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
            matchCierre(TokenType.PUNTO_COMA);
            condFor();
            matchCierre(TokenType.PUNTO_COMA);
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
            matchCierre(TokenType.PUNTO_COMA);
            condFor();
            matchCierre(TokenType.PUNTO_COMA);
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
            matchCierre(TokenType.PUNTO_COMA);
            condFor();
            matchCierre(TokenType.PUNTO_COMA);
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
