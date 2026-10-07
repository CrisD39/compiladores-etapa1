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
    //                  = { public, private } ∪ { static, void, < } ∪ FIRST(<Tipo>)
    // El '<' es FIRST de la rama nueva de método de instancia genérico
    // (<GenericidadOpcional> sin 'static' antes, ver cuerpoMiembro()).
    private static final Set<TokenType> PRIMEROS_MIEMBRO = EnumSet.of(
            TokenType.PR_PUBLIC, TokenType.PR_PRIVATE, TokenType.PR_FINAL, TokenType.PR_STATIC,
            TokenType.PR_VOID, TokenType.PR_BOOLEAN, TokenType.PR_CHAR, TokenType.PR_INT,
            TokenType.ID_CLASE, TokenType.ID_GEN, TokenType.OP_MENOR);

    // FIRST(<MetodoInterfaz>) = FIRST(<GenericidadOpcional>) ∪ FIRST(<TipoMetodo>)
    //                         = { < } ∪ PRIMEROS_TIPO_METODO
    private static final Set<TokenType> PRIMEROS_METODO_INTERFAZ = EnumSet.of(
            TokenType.PR_BOOLEAN, TokenType.PR_CHAR, TokenType.PR_INT,
            TokenType.ID_CLASE, TokenType.ID_GEN, TokenType.PR_VOID, TokenType.OP_MENOR);

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

    // Los tres modificadores de <ListaClases> (sealed/nonsealed/final) -- se
    // usa para detectar cuando aparece OTRO de estos justo donde se espera
    // "class"/"interface" (ej. "sealed sealed class X", "final sealed final
    // class X"): sin este chequeo, match(PR_CLASS) fallaba con un error
    // genérico y el modo pánico se comía toda la declaración real como
    // basura hasta el próximo "{" -- ver errorModificadorRepetido().
    private static final Set<TokenType> MODIFICADORES_CLASE = EnumSet.of(
            TokenType.PR_SEALED, TokenType.PR_NONSEALED, TokenType.PR_FINAL);

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
        registrarError(esperado);
        sincronizar();
    }

    private void registrarError(String esperado) {
        String encontrado = tokenActual.getTipo().getNombre()
                + " (\"" + tokenActual.getLexema() + "\")";
        errores.add(new ErrorSintactico(tokenActual.getLinea(), tokenActual.getLexema(), encontrado, esperado));
    }

    // Caso especial de error, sin sincronizar(): un segundo modificador
    // (sealed/nonsealed/final) donde se esperaba "class"/"interface" no se
    // trata como un token "basura" a saltear hasta el próximo "{" -- se
    // reporta un único mensaje claro y se deja el modificador repetido tal
    // cual, sin consumir nada. Así, cuando listaClases() recursivamente
    // vuelve a mirar el token actual, lo trata como un modificador nuevo
    // (sealed()/nonSealed()/finalClase() otra vez) y la declaración real que
    // sigue después se termina parseando bien, en vez de perderse en la
    // cascada de modo pánico.
    private void errorModificadorRepetido() {
        registrarError("\"class\" o \"interface\" (no se puede repetir ni combinar sealed/nonsealed/final)");
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

    // <ListaClases> ::= <Sealed> <ListaClases> | <NonSealed> <ListaClases>
    //                  | <Final> <ListaClases>
    //                  | <Clase> <ListaClases> | <Interfaz> <ListaClases> | ϵ

    // <Sealed> ::= sealed class idClase <GenericidadOpcional> <HerenciaOpcional> permits <PermitidosSealed> { <ListaMiembros> }
    //            | sealed interface idClase <GenericidadOpcional> <ExtensionOpcional> permits <PermitidosSealed> { <ListaMetodosInterfaz> }
    // <PermitidosSealed>      ::= idClase <PermitidosSealedResto>
    // <PermitidosSealedResto> ::= , idClase <PermitidosSealedResto> | ϵ
    // <NonSealed> ::= nonsealed class idClase <GenericidadOpcional> <HerenciaOpcional> { <ListaMiembros> }
    //               | nonsealed interface idClase <GenericidadOpcional> <ExtensionOpcional> { <ListaMetodosInterfaz> }
    // <Final> ::= final class idClase <GenericidadOpcional> <HerenciaOpcional> { <ListaMiembros> }
    // 'final' solo aplica a clase (y método, dentro de <Miembro>) — a
    // diferencia de Java real, esta gramática no admite 'final interface':
    // no existe tal cosa en Java (las interfaces no se sellan con final), así
    // que "final interface X" cae en el else de finalClase() y termina en el
    // error sintáctico normal de clase() esperando "class" y encontrando
    // "interface" -- no hace falta un caso especial para rechazarlo.
    // 'sealed'/'nonsealed'/'final' son alternativas separadas al principio de
    // <ListaClases> (mismo mecanismo que ya separa <Clase>/<Interfaz>): como
    // cada una arranca con una palabra clave distinta, es sintácticamente
    // imposible escribir "sealed final class" — no hace falta ningún chequeo
    // semántico para prohibir esa combinación contradictoria, la gramática ya
    // la excluye por construcción.
    private void listaClases() {
        if (actualEs(TokenType.PR_SEALED)) {
            sealed();
            listaClases();
        } else if (actualEs(TokenType.PR_NONSEALED)) {
            nonSealed();
            listaClases();
        } else if (actualEs(TokenType.PR_FINAL)) {
            finalClase();
            listaClases();
        } else if (actualEs(TokenType.PR_CLASS)) {
            clase();
            listaClases();
        } else if (actualEs(TokenType.PR_INTERFACE)) {
            interfaz();
            listaClases();
        } else {
            // ϵ — no hace nada (FOLLOW = { eof })
        }
    }

    private void finalClase() {
        match(TokenType.PR_FINAL);
        if (actualEn(MODIFICADORES_CLASE)) {
            errorModificadorRepetido();
            return;
        }
        clase(true);
    }

    // <Sealed> ::= sealed <SealedClase> | sealed <SealedInterfaz>
    private void sealed() {
        match(TokenType.PR_SEALED);
        if (actualEn(MODIFICADORES_CLASE)) {
            errorModificadorRepetido();
            return;
        }
        if (actualEs(TokenType.PR_INTERFACE)) {
            sealedInterfaz();
        } else {
            sealedClase();
        }
    }

    private void sealedClase() {
        match(TokenType.PR_CLASS);
        Token nombreClase = tokenActual;
        match(TokenType.ID_CLASE);
        Clase claseActual = crearClase(nombreClase);
        claseActual.setSealed(true);
        tablaSimbolo.insertarClase(claseActual);
        claseActual.setParametrosTipo(genericidadOpcional());
        herenciaOpcional();
        permitidosSealed(claseActual);
        match(TokenType.LLAVE_A);
        listaMiembros();
        match(TokenType.LLAVE_C);
    }

    // Mismo mecanismo que sealedClase(), sobre <ExtensionOpcional> y
    // <ListaMetodosInterfaz> en vez de <HerenciaOpcional>/<ListaMiembros>.
    private void sealedInterfaz() {
        match(TokenType.PR_INTERFACE);
        Token nombreInterfaz = tokenActual;
        match(TokenType.ID_CLASE);
        Interfaz interfazActual = new Interfaz(nombreInterfaz, tablaSimbolo);
        interfazActual.setSealed(true);
        tablaSimbolo.insertarClase(interfazActual);
        interfazActual.setParametrosTipo(genericidadOpcional());
        extensionOpcional(interfazActual);
        permitidosSealedInterfaz(interfazActual);
        match(TokenType.LLAVE_A);
        listaMetodosInterfaz(interfazActual);
        match(TokenType.LLAVE_C);
    }

    private void permitidosSealed(Clase claseActual) {
        match(TokenType.PR_PERMITS);
        Token permitido = tokenActual;
        match(TokenType.ID_CLASE);
        claseActual.agregarPermitido(permitido);
        permitidosSealedResto(claseActual);
    }

    private void permitidosSealedResto(Clase claseActual) {
        if (actualEs(TokenType.COMA)) {
            match(TokenType.COMA);
            Token permitido = tokenActual;
            match(TokenType.ID_CLASE);
            claseActual.agregarPermitido(permitido);
            permitidosSealedResto(claseActual);
        }
        // ϵ
    }

    // Mismas dos producciones que permitidosSealed()/permitidosSealedResto(),
    // pero acumulando sobre Interfaz en vez de Clase (ver esas para el
    // comentario de gramática).
    private void permitidosSealedInterfaz(Interfaz interfazActual) {
        match(TokenType.PR_PERMITS);
        Token permitido = tokenActual;
        match(TokenType.ID_CLASE);
        interfazActual.agregarPermitido(permitido);
        permitidosSealedRestoInterfaz(interfazActual);
    }

    private void permitidosSealedRestoInterfaz(Interfaz interfazActual) {
        if (actualEs(TokenType.COMA)) {
            match(TokenType.COMA);
            Token permitido = tokenActual;
            match(TokenType.ID_CLASE);
            interfazActual.agregarPermitido(permitido);
            permitidosSealedRestoInterfaz(interfazActual);
        }
        // ϵ
    }

    // <NonSealed> ::= nonsealed <NonSealedClase> | nonsealed <NonSealedInterfaz>
    private void nonSealed() {
        match(TokenType.PR_NONSEALED);
        if (actualEn(MODIFICADORES_CLASE)) {
            errorModificadorRepetido();
            return;
        }
        if (actualEs(TokenType.PR_INTERFACE)) {
            nonSealedInterfaz();
        } else {
            nonSealedClase();
        }
    }

    private void nonSealedClase() {
        match(TokenType.PR_CLASS);
        Token nombreClase = tokenActual;
        match(TokenType.ID_CLASE);
        Clase claseActual = crearClase(nombreClase);
        claseActual.setNonSealed(true);
        tablaSimbolo.insertarClase(claseActual);
        claseActual.setParametrosTipo(genericidadOpcional());
        herenciaOpcional();
        match(TokenType.LLAVE_A);
        listaMiembros();
        match(TokenType.LLAVE_C);
    }

    private void nonSealedInterfaz() {
        match(TokenType.PR_INTERFACE);
        Token nombreInterfaz = tokenActual;
        match(TokenType.ID_CLASE);
        Interfaz interfazActual = new Interfaz(nombreInterfaz, tablaSimbolo);
        interfazActual.setNonSealed(true);
        tablaSimbolo.insertarClase(interfazActual);
        interfazActual.setParametrosTipo(genericidadOpcional());
        extensionOpcional(interfazActual);
        match(TokenType.LLAVE_A);
        listaMetodosInterfaz(interfazActual);
        match(TokenType.LLAVE_C);
    }
    // <Clase> ::= class idClase <GenericidadOpcional> <HerenciaOpcional> { <ListaMiembros> }
    // Pasada 1 del EDT (ver "Acciones semánticas sobre la gramática original"
    // en analizador_semantico.md): apenas se reconoce el nombre, se crea la
    // Clase (vacía) y se registra en la tabla. Herencia/miembros quedan para
    // la Pasada 2 (fuera de alcance de este pase).
    private void clase() {
        clase(false);
    }

    private void clase(boolean esFinal) {
        match(TokenType.PR_CLASS);
        Token nombreClase = tokenActual;
        match(TokenType.ID_CLASE);
        Clase claseActual = crearClase(nombreClase);
        claseActual.setFinal(esFinal);
        tablaSimbolo.insertarClase(claseActual);
        claseActual.setParametrosTipo(genericidadOpcional());
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
        interfazActual.setParametrosTipo(genericidadOpcional());
        extensionOpcional(interfazActual);
        match(TokenType.LLAVE_A);
        listaMetodosInterfaz(interfazActual);
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

    // Construye el Metodo con placeholders (tipoRetorno=null, sin parámetros).
    // NO lo agrega todavía a la clase actual -- eso hay que hacerlo recién
    // después de parchear los parámetros reales (setParametrosAMetodoActual),
    // vía tablaSimbolo.agregarMetodoActualAClaseActual() en cada call site.
    // Antes se agregaba acá mismo, con el placeholder todavía en params=[]:
    // Clase.agregarMetodo() calcula getFirma() (nombre + tipos de parámetro)
    // en ESE momento para la key del mapa y el chequeo de ERR_METODO_DUPLICADO,
    // así que quedaba fijada como "nombre()" para siempre -- mutar
    // metodo.parametros después no reindexa el HashMap. Resultado: dos
    // sobrecargas legítimas con distintos parámetros (ej. mover(int) y
    // mover(char)) colisionaban como si fueran el mismo método.
    private void crearMetodo(Token nombre, boolean estatico, boolean esFinal) {
        Metodo metodo = new Metodo(nombre, null, estatico, new ArrayList<>());
        metodo.setFinal(esFinal);
        tablaSimbolo.setMetodoActual(metodo);
    }

    // <GenericidadOpcional> ::= < idGen > | ϵ
    // Un único parámetro de tipo (REQ-AS-010), compartida por clase,
    // interfaz y método. Devuelve lista (0 o 1 elemento) para que el modelo
    // trate igual "sin genericidad" y "con un parámetro".
    private List<Token> genericidadOpcional() {
        List<Token> parametrosTipo = new ArrayList<>();
        if (actualEs(TokenType.OP_MENOR)) {
            match(TokenType.OP_MENOR);
            parametrosTipo.add(tokenActual);
            match(TokenType.ID_GEN);
            match(TokenType.OP_MAYOR);
        }
        return parametrosTipo;
    }

    // <HerenciaOpcional> ::= extends <TipoReferencia> | implements <ListaInterfaces> | ϵ
    // 'extends' sigue aceptando un único tipo (una clase no puede heredar de
    // varias clases, ni en Java real) y sigue siendo mutuamente excluyente
    // con 'implements' en la misma clase (ver sintError47.java) -- lo único
    // que cambia es que 'implements' ahora acepta una lista separada por
    // coma (Logro 2: "class X implements A, B"), antes limitado a un único
    // <TipoReferencia> (ver sintError52.java, reescrito para la nueva
    // gramática).
    private void herenciaOpcional() {
        if (actualEs(TokenType.PR_EXTENDS)) {
            match(TokenType.PR_EXTENDS);
            Token padre = tokenActual; // idClase del padre, antes de que tipoReferencia() lo consuma
            tipoReferencia();
            tablaSimbolo.getClaseActual().setHerencia(padre);
        } else if (actualEs(TokenType.PR_IMPLEMENTS)) {
            match(TokenType.PR_IMPLEMENTS);
            listaInterfacesImplementadas(tablaSimbolo.getClaseActual());
        } else {
            // ϵ — no hace nada
        }
    }

    // <ListaInterfaces>      ::= idClase <TipoGenericoOpcional> <ListaInterfacesResto>
    // <ListaInterfacesResto> ::= , idClase <TipoGenericoOpcional> <ListaInterfacesResto> | ϵ
    // (el idClase + <TipoGenericoOpcional> de cada entrada se consumen vía
    // tipoReferencia(), igual que en el resto de la gramática).
    private void listaInterfacesImplementadas(Clase claseActual) {
        Token interfaz = tokenActual;
        tipoReferencia();
        claseActual.agregarInterfaz(interfaz);
        listaInterfacesImplementadasResto(claseActual);
    }

    private void listaInterfacesImplementadasResto(Clase claseActual) {
        if (actualEs(TokenType.COMA)) {
            match(TokenType.COMA);
            Token interfaz = tokenActual;
            tipoReferencia();
            claseActual.agregarInterfaz(interfaz);
            listaInterfacesImplementadasResto(claseActual);
        }
        // ϵ
    }

    // <ExtensionOpcional> ::= extends <ListaInterfaces> | ϵ
    // Mismas dos producciones que listaInterfacesImplementadas()/
    // ...Resto(), pero acumulando sobre Interfaz.agregarHerencia() en vez de
    // Clase.agregarInterfaz() -- ahora también admite varias (Logro 2:
    // "interface X extends Y, Z"), antes un único <TipoReferencia>.
    private void extensionOpcional(Interfaz interfazActual) {
        if (actualEs(TokenType.PR_EXTENDS)) {
            match(TokenType.PR_EXTENDS);
            listaInterfacesExtendidas(interfazActual);
        } else {
            // ϵ — no hace nada
        }
    }

    private void listaInterfacesExtendidas(Interfaz interfazActual) {
        Token padre = tokenActual;
        tipoReferencia();
        interfazActual.agregarHerencia(padre);
        listaInterfacesExtendidasResto(interfazActual);
    }

    private void listaInterfacesExtendidasResto(Interfaz interfazActual) {
        if (actualEs(TokenType.COMA)) {
            match(TokenType.COMA);
            Token padre = tokenActual;
            tipoReferencia();
            interfazActual.agregarHerencia(padre);
            listaInterfacesExtendidasResto(interfazActual);
        }
        // ϵ
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
    private void listaMetodosInterfaz(Interfaz interfazActual) {
        if (actualEn(PRIMEROS_METODO_INTERFAZ)) {
            metodoInterfaz(interfazActual);
            listaMetodosInterfaz(interfazActual);
        } else {
            // ϵ — no hace nada
        }
    }

    // <Miembro> ::= <Visibilidad> <FinalOpcional> <CuerpoMiembro>
    private void miembro() {
        visibilidad();
        boolean esFinal = finalOpcional();
        cuerpoMiembro(esFinal);
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

    // <FinalOpcional> ::= final | ϵ
    private boolean finalOpcional() {
        if (actualEs(TokenType.PR_FINAL)) {
            match(TokenType.PR_FINAL);
            return true;
        }
        return false;
    }

    // <CuerpoMiembro> ::= static <GenericidadOpcional> <TipoMetodo> idMetVar <ArgsFormales> <Bloque>
    //                 |  <GenericidadOpcional> <TipoMetodo> idMetVar <ArgsFormales> <Bloque>   (sin 'static', dispara con '<')
    //                 |  void idMetVar <ArgsFormales> <Bloque>
    //                 |  <TipoPrimitivo> <DimensionesOpcionales> idMetVar <RestoMiembro>
    //                 |  idGen <DimensionesOpcionales> idMetVar <RestoMiembro>
    //                 |  idClase <TrasIdClaseMiembro>
    // El "public" que antes marcaba al constructor pasó a ser parte de
    // <Visibilidad>; el constructor (idClase <ArgsFormales> <Bloque>) queda con
    // el mismo prefijo "idClase" que un atributo/método de tipo clase, así que
    // se factoriza en profundidad en trasIdClaseMiembro() (ver "Factorización
    // de <Miembro>" en el documento).
    // esFinal viaja desde <FinalOpcional> (ya consumido en miembro()) hasta el
    // punto donde se sabe si esto termina siendo un método (se guarda en el
    // Metodo real) o un atributo/constructor (no hay dónde guardarlo todavía:
    // 'final' ahí es error sintáctico, ver restoMiembro()/trasIdClaseMiembro()).
    private void cuerpoMiembro(boolean esFinal) {
        if (actualEs(TokenType.PR_STATIC)) {
            match(TokenType.PR_STATIC);
            List<Token> parametrosTipoMetodo = genericidadOpcional();
            metodoConGenericidadPropia(true, esFinal, parametrosTipoMetodo);
        } else if (actualEs(TokenType.OP_MENOR)) {
            // Método de instancia con parámetro(s) de tipo propio(s):
            // <T> TipoMetodo idMetVar(...) {...}. Dispara directo con '<':
            // nunca puede terminar siendo atributo ni constructor, así que no
            // pasa por restoMiembro()/trasIdClaseMiembro().
            List<Token> parametrosTipoMetodo = genericidadOpcional();
            metodoConGenericidadPropia(false, esFinal, parametrosTipoMetodo);
        } else if (actualEs(TokenType.PR_VOID)) {
            match(TokenType.PR_VOID);
            Token nombreMetodo = tokenActual;
            match(TokenType.ID_MET_VAR);
            crearMetodo(nombreMetodo, false, esFinal);
            List<Parametro> params = argsFormales();
            tablaSimbolo.setParametrosAMetodoActual(params);
            tablaSimbolo.agregarMetodoActualAClaseActual();
            bloque();
        } else if (actualEn(PRIMEROS_TIPO_PRIMITIVO)) {
            Token tipoTok = tipoPrimitivo();
            dimensionesOpcionales();
            Token nombreMiembro = tokenActual;
            match(TokenType.ID_MET_VAR);
            restoMiembro(nombreMiembro, new Tipo(tipoTok), esFinal);
        } else if (actualEs(TokenType.ID_GEN)) {
            Token tipoTok = tokenActual;
            match(TokenType.ID_GEN);
            dimensionesOpcionales();
            Token nombreMiembro = tokenActual;
            match(TokenType.ID_MET_VAR);
            restoMiembro(nombreMiembro, new Tipo(tipoTok), esFinal);
        } else if (actualEs(TokenType.ID_CLASE)) {
            Token idClaseTok = tokenActual;
            match(TokenType.ID_CLASE);
            trasIdClaseMiembro(idClaseTok, esFinal);
        } else {
            error("un miembro de clase (\"static\", \"void\", \"<\" o un tipo)");
        }
    }

    // Cuerpo común a "static <GenericidadOpcional> <TipoMetodo> ..." y
    // "<GenericidadOpcional> <TipoMetodo> ..." (sin 'static') -- ambas ramas
    // solo difieren en 'estatico' y en si matchearon 'static' antes.
    private void metodoConGenericidadPropia(boolean estatico, boolean esFinal, List<Token> parametrosTipoMetodo) {
        Tipo tipoRetorno = tipoMetodo();
        Token nombreMetodo = tokenActual;
        match(TokenType.ID_MET_VAR);
        crearMetodo(nombreMetodo, estatico, esFinal);
        List<Parametro> params = argsFormales();
        tablaSimbolo.setTipoRetornoAMetodoActual(tipoRetorno);
        tablaSimbolo.setParametrosAMetodoActual(params);
        tablaSimbolo.setParametrosTipoAMetodoActual(parametrosTipoMetodo);
        tablaSimbolo.agregarMetodoActualAClaseActual();
        bloque();
    }

    // <TrasIdClaseMiembro> ::= <ArgsFormales> <Bloque>
    //                       |  <TipoGenericoOpcional> <DimensionesOpcionales> idMetVar <RestoMiembro>
    // Tras "idClase", "(" => era el constructor (idClase <ArgsFormales> <Bloque>);
    // cualquier otra cosa => era un atributo o método cuyo tipo es esa clase
    // (Foo bar; | Foo<X> bar; | Foo bar(...) {...}), con <TipoGenericoOpcional>
    // y <DimensionesOpcionales> anulables hasta idMetVar.
    private void trasIdClaseMiembro(Token idClassToken, boolean esFinal) {
        if (actualEs(TokenType.PAR_A)) {
            // "(" pegado, es el constructor. 'final' no aplica a constructores
            // (no son métodos ni se redefinen) — igual que en atributo, error
            // sintáctico en vez de dejarlo pasar sin guardarlo en ningún lado.
            if (esFinal) {
                error("un método (\"final\" no se puede aplicar a un constructor)");
                return;
            }
            List<Parametro> params = argsFormales();
            bloque();
            Constructor constructor = new Constructor(idClassToken, params);
            tablaSimbolo.getClaseActual().agregarConstructor(constructor);
        } else {
            Tipo tipo = new Tipo(idClassToken, tipoGenericoOpcional());
            dimensionesOpcionales();
            Token nombreMiembro = tokenActual;
            match(TokenType.ID_MET_VAR);
            restoMiembro(nombreMiembro, tipo, esFinal);
        }
    }

    // <RestoMiembro> ::= ;
    //                |  <ArgsFormales> <Bloque>
    //                |  <OperadorAsignacion> <ExpresionCompuesta> ;
    // La tercera rama (REQ-AS-011) es un atributo con inicializador
    // (int x = 5;). FIRST disjuntos con las otras dos ({ ; } / { ( } / { = }):
    // no hace falta factorizar nada, "=" ya alcanza para decidir.
    // nombreMiembro/tipo llegan por parámetro (heredado) desde
    // cuerpoMiembro()/trasIdClaseMiembro(): son el tipo y el idMetVar ya
    // consumidos antes de saber si es atributo o método — recién acá se
    // construye el objeto (Estrategia B, ver analizador_semantico.md).
    private void restoMiembro(Token nombreMiembro, Tipo tipo, boolean esFinal) {
        if (actualEs(TokenType.PUNTO_COMA)) {
            // 'final' no aplica a atributos (solo clase/interfaz/método, ver
            // Logro 1 en analizador_semantico.md) — error sintáctico en vez de
            // dejarlo pasar sin guardarlo (Atributo no tiene campo 'final').
            if (esFinal) {
                error("un método (\"final\" no se puede aplicar a un atributo)");
                return;
            }
            match(TokenType.PUNTO_COMA);
            tablaSimbolo.getClaseActual().agregarAtributo(new Atributo(nombreMiembro, tipo));
        } else if (actualEs(TokenType.PAR_A)) {
            crearMetodo(nombreMiembro, false, esFinal);
            List<Parametro> params = argsFormales();
            tablaSimbolo.setParametrosAMetodoActual(params);
            tablaSimbolo.setTipoRetornoAMetodoActual(tipo);
            tablaSimbolo.agregarMetodoActualAClaseActual();
            bloque();
        } else if (actualEs(TokenType.OP_ASIGN)) {
            if (esFinal) {
                error("un método (\"final\" no se puede aplicar a un atributo)");
                return;
            }
            operadorAsignacion();
            expresionCompuesta();
            matchCierre(TokenType.PUNTO_COMA);
            tablaSimbolo.getClaseActual().agregarAtributo(new Atributo(nombreMiembro, tipo));
        } else {
            error("\";\", \"=\" (atributo) o \"(\" (método)");
        }
    }

    // <MetodoInterfaz> ::= <GenericidadOpcional> <TipoMetodo> idMetVar <ArgsFormales> ;
    // A diferencia de un método de clase (crearMetodo() + parcheo en dos
    // pasadas vía TablaSimbolos, porque ahí falta parsear el <Bloque>), acá
    // no hay cuerpo -- tipoRetorno/params ya están completos antes de
    // construir el Metodo, Estrategia B pura (mismo criterio que
    // Atributo/Parametro/Constructor).
    private void metodoInterfaz(Interfaz interfazActual) {
        List<Token> parametrosTipoMetodo = genericidadOpcional();
        Tipo tipoRetorno = tipoMetodo();
        Token nombreMetodo = tokenActual;
        match(TokenType.ID_MET_VAR);
        List<Parametro> params = argsFormales();
        matchCierre(TokenType.PUNTO_COMA);
        Metodo metodo = new Metodo(nombreMetodo, tipoRetorno, false, params);
        metodo.setParametrosTipoPropios(parametrosTipoMetodo);
        interfazActual.agregarMetodo(metodo);
    }

    // <TipoMetodo> ::= <Tipo> | void
    // null representa "void" (ver Metodo.tipoRetorno) -- no es un Tipo de valor.
    private Tipo tipoMetodo() {
        if (actualEs(TokenType.PR_VOID)) {
            match(TokenType.PR_VOID);
            return null;
        } else if (actualEn(PRIMEROS_TIPO)) {
            return tipo();
        } else {
            error("un tipo o \"void\"");
            return null;
        }
    }

    // <Tipo> ::= <TipoBase> <DimensionesOpcionales>
    // DimensionesOpcionales se consume pero no se guarda todavía (ver
    // "Jerarquía Tipo" en analizador_semantico.md).
    private Tipo tipo() {
        Tipo t = tipoBase();
        dimensionesOpcionales();
        return t;
    }

    // <TipoBase> ::= <TipoPrimitivo> | <TipoReferencia> | idGen
    private Tipo tipoBase() {
        if (actualEn(PRIMEROS_TIPO_PRIMITIVO)) {
            return new Tipo(tipoPrimitivo());
        } else if (actualEs(TokenType.ID_CLASE)) {
            return tipoReferencia();
        } else if (actualEs(TokenType.ID_GEN)) {
            Token t = tokenActual;
            match(TokenType.ID_GEN);
            return new Tipo(t);
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
    private Tipo tipoReferencia() {
        Token t = tokenActual;
        match(TokenType.ID_CLASE);
        return new Tipo(t, tipoGenericoOpcional());
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
    // Devuelve el argumento genérico, o null si no hay "<...>".
    private Tipo tipoGenericoOpcional() {
        if (actualEs(TokenType.OP_MENOR)) {
            match(TokenType.OP_MENOR);
            Tipo argumento = instanciadoOParametrico();
            match(TokenType.OP_MAYOR);
            return argumento;
        }
        return null;
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
    // Devuelve el argumento como Tipo (recursivo) para que el semántico pueda
    // validar cada nivel (Logro 5).
    private Tipo instanciadoOParametrico() {
        Token t = tokenActual;
        if (actualEs(TokenType.ID_GEN)) {
            match(TokenType.ID_GEN);
            return new Tipo(t);
        } else if (actualEs(TokenType.ID_CLASE)) {
            match(TokenType.ID_CLASE);
            return new Tipo(t, tipoGenericoOpcional());
        } else {
            error("idGen o idClase");
            return null;
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
        Tipo tipo = tipo();
        Token nombre = tokenActual;
        match(TokenType.ID_MET_VAR);
        return new Parametro(nombre, tipo);
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
