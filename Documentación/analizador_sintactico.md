# Analizador Sintáctico MiniJava

Este documento describe el **funcionamiento** del analizador sintáctico de MiniJava
(etapa 2): qué recibe, cómo lo procesa y qué produce. La sección "Gramática"
reproduce las producciones de partida (tomadas de la cátedra); el resto del
documento describe el mecanismo que la recorre.

> Estado: documento de diseño. La gramática de la sección "Gramática" es la de
> partida, tal como la entrega la cátedra — todavía no está verificada como
> LL(1) (hay producciones con recursión a izquierda, ver la nota al final de
> esa sección) y se irá ajustando en revisiones sucesivas de este documento.
> El código en `Model/AnalizadorSintactico*.java` es un esqueleto inicial con
> pendientes (ver "Estado actual del código"). Este documento fija el
> contrato antes de terminar la implementación.

## Propósito

El analizador sintáctico toma la secuencia de tokens que reconoce el analizador
léxico y decide si esa secuencia es una derivación válida de la gramática de
MiniJava. No construye todavía un AST ni hace chequeo semántico (eso queda para
etapas posteriores): el alcance de esta etapa es aceptar/rechazar el programa
sintácticamente y reportar los errores sintácticos que encuentre.

## Requerimientos

Requisitos de esta etapa, con el mismo criterio de numeración que
`Informe Analizador Léxico.md` (`REQ-<módulo>-<número>`), pero con tres dígitos
para no pisarse con los `REQ-MP-01..08` ya usados ahí — son documentos
distintos y conviene poder citarlos sin ambigüedad.

### Módulo Principal

| Código | Descripción |
| --- | --- |
| REQ-MP-001 | De manera similar a la etapa anterior, va a ser necesario un módulo principal cliente tanto del léxico como del sintáctico, que maneje la interfaz con el usuario y reporte los errores detectados por ambos analizadores. En caso de no detectarse ningún error, debe indicar que el análisis fue exitoso. |

### Analizador Sintáctico

| Código | Descripción |
| --- | --- |
| REQ-AS-001 | El analizador sintáctico va a ser descendente recursivo. Para eso hay que asegurar que la gramática sea LL(1) — ver detalle debajo. |
| REQ-AS-002 | El analizador sintáctico debe consumir los tokens que le provee el analizador léxico bajo demanda, de a uno por vez (ver "Relación con el Analizador Léxico"), sin necesitar volver sobre un token ya consumido. |
| REQ-AS-003 | Ante un error sintáctico, el analizador debe reportarlo (línea, token encontrado, token(s) esperado(s) — ver "Manejo de errores sintácticos") e intentar continuar el análisis en lugar de abortar en el primer error, para poder informar más de uno por corrida, en la misma línea que el léxico con los suyos (REQ-AL-22 de la etapa 1). |
| REQ-AS-004 | Al terminar de derivar el símbolo inicial de la gramática, el analizador debe verificar que no queden tokens sin consumir salvo el de fin de archivo; si sobran, es un error sintáctico (ver "`start()`" en "Estrategia de análisis"). |
| REQ-AS-005 | El analizador sintáctico debe aceptar **expresiones lambda** con sintaxis similar a la de Java (`params -> cuerpo`). A diferencia de Java: los parámetros no declaran tipo explícito, el cuerpo es **una única expresión** (no se admite cuerpo entre llaves) y se permite cualquier cantidad de parámetros, incluido ninguno (`() -> expr`). La prohibición de capturar variables locales, parámetros o `this` dentro de la lambda es una restricción **semántica** (requiere resolución de nombres/alcances): queda anotada acá pero se verifica en una etapa posterior; el sintáctico solo valida la forma. Ver detalle debajo. |
| REQ-AS-006 | El analizador sintáctico debe aceptar la declaración de variables locales en la forma clásica de Java: sin `var`, indicando el tipo (ejemplo `int x;`). Además debe admitir declarar varias variables e inicializarlas en una misma sentencia (ejemplo `int x,y,z = 10;`, un valor común al final para toda la lista; a qué variables les llega es semántico). **Implementado** — ver "Gramática Expandida (Logros)" y "Factorización de `<Sentencia>`". |
| REQ-AS-007 | El analizador sintáctico debe permitir indicar la visibilidad de métodos, atributos y constructores de forma implícita o explícita: al declarar un miembro se puede omitir la visibilidad o indicar explícitamente `public` o `private`. **Implementado** — ver "Gramática Expandida (Logros)" y "Factorización de `<Miembro>`". |
| REQ-AS-008 | El compilador no finaliza la ejecución ante el primer error sino que se recupera (modo pánico) y es capaz de reportar otros errores. Para eso, al encontrar un error, el analizador descarta la entrada hasta encontrar un token de sincronización que permita reanudar el análisis: se espera que se sincronice con el siguiente punto y coma, llave de cierre, o llave que abre según el contexto (ver "Manejo de errores sintácticos"). |
| REQ-AS-009 | El analizador sintáctico debe aceptar sentencias `for` similares a las de Java, en dos formas: la estándar con separadores `;` y la de iteradores (for-each) con `:`. Se restringe cada sección del `for` a una sola sentencia/expresión (no listas separadas por coma). **Implementado** — ver "Gramática Expandida (Logros)" y "Factorización de `<For>`". Las tres secciones de la forma clásica son opcionales (`for (;;)`, como en Java); el for-each no admite `var` (sólo tipo explícito). |
| REQ-AS-010 | El analizador sintáctico debe permitir tipos genéricos anidados y la notación diamante (`<>`), de forma similar a Java. Una clase o interfaz sigue limitada a un único parámetro de tipo. La notación diamante solo puede utilizarse al instanciar una clase genérica. **Implementado** — ver "Gramática Expandida (Logros)". La declaración de clase/interfaz (`<GenericidadOpcional>`) no cambió: sigue con un único parámetro de tipo. |
| REQ-AS-011 | El analizador sintáctico debe permitir inicializar los atributos en el momento de su declaración, al igual que en Java. **Implementado** — ver "Gramática Expandida (Logros)" (`<RestoMiembro>`). |
| REQ-AS-012 | El analizador sintáctico debe permitir la inicialización de arreglos al momento de su construcción, como en Java. Solo se admite como parte de una expresión de creación que utiliza `new`. **Implementado** — ver "Gramática Expandida (Logros)" (`<DimensionesNew>` / `<InicializadorArreglo>`). |
| REQ-AS-013 | El analizador sintáctico debe permitir el operador condicional ternario dentro de las expresiones, como en Java (`condición ? expr_verdadera : expr_falsa`). **Implementado** — ver "Gramática Expandida (Logros)" y "Precedencia del operador ternario". |
| REQ-AS-014 | El analizador sintáctico debe permitir los operadores unarios postfijos de incremento (`++`) y decremento (`--`). **Implementado** — ver "Gramática Expandida (Logros)" y "Postfijo `++`/`--`". |

**Detalle de REQ-AS-001 — la gramática de MiniJava no es LL(1).** La gramática
de partida (sección "Gramática", tal como la entrega la cátedra) no cumple la
condición LL(1) que necesita un descenso recursivo predictivo. Antes de poder
escribir los métodos `analizar<NoTerminal>()` hay que identificar, para cada
no terminal, cuáles de estas dos causas aplican:

- **Producciones con recursión a izquierda**: un no terminal `<X>` con una
  producción de la forma `<X> ::= <X> α`. Un descenso recursivo no puede
  recorrerla directamente: `analizarX()` se llamaría a sí mismo antes de
  consumir ningún token, y no terminaría nunca. Ya están identificadas tres en
  la sección "Gramática" (`<ListaArgsFormales>`, `<ExpresionCompuesta>`,
  `<Referencia>`) — quedan como lista de partida, no cerrada hasta revisar el
  resto.
- **Producciones de un no terminal cuyas partes derechas no están
  factorizadas**: un no terminal `<X> ::= α | β | ...` donde el FIRST de dos o
  más alternativas comparte algún token. Ahí, al ver ese token como
  lookahead, el parser no puede decidir con un solo token de anticipación cuál
  alternativa tomar — es la otra forma (además de la recursión izquierda) de
  romper LL(1). Queda pendiente de confirmar contra el FIRST real de cada
  alternativa (`<Miembro>` y `<Sentencia>` son los primeros candidatos a
  revisar, según ya señala la nota de la sección "Gramática").

Técnica a aplicar por cada causa:

- Para la recursión a izquierda: **eliminación de recursión a izquierda**
  (la forma vista en la materia: `<X> ::= <X> α | β` se reescribe como
  `<X> ::= β <X'>` y `<X'> ::= α <X'> | ϵ`, introduciendo un no terminal
  auxiliar que consume las repeticiones a derecha en vez de a izquierda).
- Para las partes derechas no factorizadas: **factorización a izquierda**,
  simple o profunda según haga falta — simple cuando alcanza con extraer el
  prefijo común una sola vez; profunda cuando, después de extraerlo, las
  alternativas resultantes todavía comparten otro prefijo y hay que repetir el
  proceso un nivel más adentro.

Por ahora este es el diagnóstico y las técnicas a usar; la aplicación
concreta, no terminal por no terminal, se va a ir volcando como revisiones de
la sección "Gramática" (dejando registro de qué se reescribió y por qué, como
ya se aclaró ahí).

**Detalle de REQ-AS-005 — forma sintáctica de la lambda.** Especificación de la
forma; la gramática concreta está en "Gramática Expandida (Logros)" y ya está
implementada en `AnalizadorSintacticoImpl`:

- **Forma aceptada**: `<Lambda> ::= <ParamsLambda> -> <Expresion>`. El cuerpo
  reusa `<Expresion>` de la gramática (una sola expresión); no hay alternativa
  con `{ ... }`.
- **Parámetros sin tipo**: lista de `idMetVar` separados por `,`,
  opcionalmente entre paréntesis; con cero parámetros el `( )` es
  obligatorio (`() -> expr`). A diferencia de `<ArgFormal> ::= <Tipo> idMetVar`,
  acá nunca aparece `<Tipo>`.
- **Dónde enchufa**: la lambda es una forma de expresión; entra como
  alternativa de `<Operando>`. Usa `->` como token del léxico (`ARROW`) y sumó
  los no terminales `<Lambda>` / `<ParamsLambda>` (que al factorizar se vuelven
  `<TrasId>` / `<TrasParen>` / `<TrasParenId>` / `<DecidirTrasCierre>` /
  `<ListaIdLambda>`).
- **Conflicto LL(1) conocido**: el prefijo `(` es ambiguo entre
  `( <Expresion> )` (`<ExpresionParentizada>`), `( ) ->` y
  `( idMetVar , ... ) ->`. Con un solo token de lookahead no se distingue una
  lambda parentizada de una expresión parentizada. Es el punto más difícil de
  factorizar de esta extensión; la solución concreta está en "Factorización del
  prefijo `(` — lambda vs. expresión parentizada" (postergar la decisión hasta
  después del `)` que cierra).
- **Restricción de captura / `this`**: es semántica (requiere resolución de
  nombres y alcances), fuera del alcance de esta etapa. Queda anotada en el
  requerimiento y se verifica en la etapa semántica posterior; el sintáctico
  solo valida la forma de la lambda.

**Detalle de REQ-AS-007 — visibilidad de miembros.** La gramática concreta está
en "Gramática Expandida (Logros)" y ya está implementada:

- **Forma**: `<Visibilidad> ::= public | private | ϵ` como prefijo opcional de
  todo `<Miembro>` (atributo, método y constructor). Omitirla = visibilidad
  implícita; qué visibilidad implica y si `private` es legal en cada contexto se
  resuelve en la etapa semántica.
- **Efecto sobre el constructor**: deja de escribirse `public idClase (...)` con
  `public` obligatorio; pasa a `<Visibilidad> idClase <ArgsFormales> <Bloque>`
  (con `public`, `private` o nada).
- **Conflicto LL(1) que introduce**: al no ser ya `public` el token que marca al
  constructor, tras `<Visibilidad>` el prefijo `idClase` queda compartido entre
  constructor (`idClase (`) y atributo/método de tipo clase (`idClase <` /
  `idClase [` / `idClase idMetVar`). Se factoriza en profundidad sobre `idClase`
  (`<TrasIdClaseMiembro>`), misma técnica que `<RestoNewIdClase>` y
  `<SentIdClase>`. Detalle en "Factorización de `<Miembro>`".
- **Token nuevo en el léxico**: `private` no era palabra clave (`public` sí,
  `PR_PUBLIC`); hay que sumar `PR_PRIVATE` a `TokenType` y `TablaPalabrasClave`.
- **Interfaces**: `<MetodoInterfaz>` no cambia — los métodos de interfaz quedan
  implícitamente `public`, sin sintaxis de visibilidad.

## Gramática

Gramática de partida de MiniJava, copiada tal cual de la cátedra
(`Reglas de Sintaxis MiniJava 2026.pdf`), en la misma notación BNF que ya
introduce `SIntaxis.md` (`terminal` en minúscula, `<NoTerminal>` con mayúscula
inicial, `ϵ` cadena vacía, `<X>::=α|β` abreviatura de dos producciones). El no
terminal inicial es `<Inicial>`.

Se transcribe completa acá, sin modificar todavía ninguna producción, para
tener un único punto de partida versionado sobre el cual ir marcando los
ajustes (ver la nota al final de la sección).

```
<Inicial> ::= <ListaClases> eof

<ListaClases> ::= <Clase> <ListaClases> | <Interfaz> <ListaClases> | ϵ

<Clase> ::= class idClase <GenericidadOpcional> <HerenciaOpcional> { <ListaMiembros> }

<Interfaz> ::= interface idClase <GenericidadOpcional> <ExtensionOpcional> { <ListaMetodosInterfaz> }

<GenericidadOpcional> ::= < idGen > | ϵ

<HerenciaOpcional> ::= extends <TipoReferencia> | implements <TipoReferencia> | ϵ

<ExtensionOpcional> ::= extends <TipoReferencia> | ϵ

<ListaMiembros> ::= <Miembro> <ListaMiembros> | ϵ

<ListaMetodosInterfaz> ::= <MetodoInterfaz> <ListaMetodosInterfaz> | ϵ

<Miembro> ::= <Atributo> | <Metodo> | <Constructor>

<Atributo> ::= <Tipo> idMetVar ;

<Metodo> ::= <ModificadorOpcional> <TipoMetodo> idMetVar <ArgsFormales> <Bloque>

<MetodoInterfaz> ::= <TipoMetodo> idMetVar <ArgsFormales> ;

<Constructor> ::= public idClase <ArgsFormales> <Bloque>

<ModificadorOpcional> ::= static | ϵ

<TipoMetodo> ::= <Tipo> | void

<Tipo> ::= <TipoBase> <DimensionesOpcionales>

<TipoBase> ::= <TipoPrimitivo> | <TipoReferencia> | idGen

<DimensionesOpcionales> ::= [ ] <DimensionesOpcionales> | ϵ

<TipoReferencia> ::= idClase <TipoGenericoOpcional>

<TipoPrimitivo> ::= boolean | char | int

<TipoGenericoOpcional> ::= < <InstanciadoOParametrico> > | ϵ

<InstanciadoOParametrico> ::= idGen | idClase

<ArgsFormales> ::= ( <ListaArgsFormalesOpcional> )

<ListaArgsFormalesOpcional> ::= <ListaArgsFormales> | ϵ

<ListaArgsFormales> ::= <ArgFormal>
<ListaArgsFormales> ::= <ListaArgsFormales> , <ArgFormal>

<ArgFormal> ::= <Tipo> idMetVar

<Bloque> ::= { <ListaSentencias> }

<ListaSentencias> ::= <Sentencia> <ListaSentencias> | ϵ

<Sentencia> ::= ;
<Sentencia> ::= <Asignacion> ;
<Sentencia> ::= <Llamada> ;
<Sentencia> ::= <VarLocal> ;
<Sentencia> ::= <Return> ;
<Sentencia> ::= <If>
<Sentencia> ::= <While>
<Sentencia> ::= <Bloque>

<Asignacion> ::= <Expresion>

<Llamada> ::= <Expresion>

<VarLocal> ::= var idMetVar = <ExpresionCompuesta>

<Return> ::= return <ExpresionOpcional>

<ExpresionOpcional> ::= <Expresion> | ϵ

<If> ::= if ( <Expresion> ) <Sentencia>
<If> ::= if ( <Expresion> ) <Sentencia> else <Sentencia>

<While> ::= while ( <Expresion> ) <Sentencia>

<Expresion> ::= <ExpresionCompuesta> <OperadorAsignacion> <ExpresionCompuesta>
<Expresion> ::= <ExpresionCompuesta>

<OperadorAsignacion> ::= =

<ExpresionCompuesta> ::= <ExpresionCompuesta> <OperadorBinario> <ExpresionBasica>
<ExpresionCompuesta> ::= <ExpresionBasica>

<OperadorBinario> ::= || | && | == | != | < | > | <= | >= | + | - | * | / | %

<ExpresionBasica> ::= <OperadorUnario> <Operando>
<ExpresionBasica> ::= <Operando>

<OperadorUnario> ::= + | - | !

<Operando> ::= <Primitivo>
<Operando> ::= <Referencia>

<Primitivo> ::= true | false | intLiteral | charLiteral | null

<Referencia> ::= <Primario>
<Referencia> ::= <Referencia> <VarEncadenada>
<Referencia> ::= <Referencia> <MetodoEncadenado>
<Referencia> ::= <Referencia> <AccesoArreglo>

<Primario> ::= this
<Primario> ::= stringLiteral
<Primario> ::= <AccesoVar>
<Primario> ::= <CrearArreglo>
<Primario> ::= <LlamadaConstructor>
<Primario> ::= <LlamadaMetodo>
<Primario> ::= <LlamadaMetodoEstatico>
<Primario> ::= <ExpresionParentizada>

<AccesoVar> ::= idMetVar

<CrearArreglo> ::= new <TipoBase> <DimensionesConTamanio>

<LlamadaConstructor> ::= new <TipoReferencia> <ArgsActuales>

<ExpresionParentizada> ::= ( <Expresion> )

<LlamadaMetodo> ::= idMetVar <ArgsActuales>

<LlamadaMetodoEstatico> ::= idClase . idMetVar <ArgsActuales>

<DimensionesConTamanio> ::= [ <Expresion> ] <DimensionesConTamanio> | [ <Expresion> ]

<ArgsActuales> ::= ( <ListaExpsOpcional> )

<ListaExpsOpcional> ::= <ListaExps> | ϵ

<ListaExps> ::= <Expresion>
<ListaExps> ::= <Expresion> , <ListaExps>

<VarEncadenada> ::= . idMetVar

<MetodoEncadenado> ::= . idMetVar <ArgsActuales>

<AccesoArreglo> ::= [ <Expresion> ]
```

**Nota — todavía no es LL(1) tal como está.** Esta es la gramática que da la
cátedra para describir el lenguaje, no la forma que necesita un descenso
recursivo predictivo: tiene recursión a izquierda, alternativas sin factorizar
y al menos una ambigüedad real. El análisis completo, regla por regla, está en
"Reglas que violan LL(1) — análisis de la gramática de partida", más abajo en
esta misma sección.

La **producción de expresión lambda** (`REQ-AS-005`),
`<Lambda> ::= <ParamsLambda> -> <Expresion>` como alternativa de expresión (con
`<ParamsLambda>` una lista de `idMetVar` sin tipo, opcionalmente entre `( )`,
obligatorio `( )` con cero parámetros), **ya está resuelta e implementada**: la
gramática factorizada está en "Gramática Expandida (Logros)" y el conflicto LL(1)
del prefijo `(` (`( <Expresion> )` vs. `( ) ->` vs. `( idMetVar , ... ) ->`) se
trata en "Factorización del prefijo `(` — lambda vs. expresión parentizada". Este
bloque de gramática de partida no la incluye a propósito (es el punto de partida
sin extensiones). Ver "Detalle de REQ-AS-005" en "Requerimientos".

Las **variables locales clásicas** (`REQ-AS-006`) —declaración sin `var`, con
tipo explícito y varias variables por sentencia con un valor común
(`int x, y, z = 10;`)— **ya están reflejadas** en "Gramática Expandida
(Logros)" (`<Sentencia>` con arranque `<TipoPrimitivo>` / `idGen` / `idClase`,
más `<SentIdClase>` / `<RestoDeclLocal>` / `<MasIdsLocal>` / `<InitLocalOpc>`) y
en el código. El detalle del conflicto del prefijo `idClase` está en
"Factorización de `<Sentencia>`". La forma `var idMetVar = ...` queda intacta.

La **visibilidad de miembros** (`REQ-AS-007`) —`public` / `private` opcional en
atributos, métodos y constructores— **ya está reflejada** en "Gramática
Expandida (Logros)": `<Visibilidad> ::= public | private | ϵ` como prefijo común
de `<Miembro>`, con `<Constructor>` pasando de `public idClase (...)` obligatorio
a `<Visibilidad> idClase (...)`. La factorización profunda sobre `idClase` que
esto obliga (constructor vs. tipo clase, ya que `public` deja de marcar al
constructor) está en "Factorización de `<Miembro>`". `<Atributo>`, `<Metodo>`,
`<Constructor>` y `<ModificadorOpcional>` de la gramática de partida siguen sin
usarse en la expandida (ya inlineados desde el Paso 2.2).

La sentencia **`for`** (`REQ-AS-009`) —forma estándar con separadores `;` y
forma de iteradores (for-each) con `:`, con una sola sentencia/expresión por
sección— **ya está reflejada** en "Gramática Expandida (Logros)":
`<Sentencia> ::= <For>`, con `<For> ::= for ( <ClausulasFor> ) <Sentencia>` (el
cuerpo es `<Sentencia>`, igual que `<If>`/`<While>`, no un `<Bloque>`
obligatorio). Las dos formas comparten el prefijo `<Tipo> idMetVar`, que se
factoriza igual que en el resto del documento; el detalle está en
"Factorización de `<For>`".

Los **genéricos anidados y la notación diamante** (`REQ-AS-010`) —
`Caja<Lista<Item>>` y `new Caja<>()`— **ya están reflejados** en "Gramática
Expandida (Logros)": `<InstanciadoOParametrico>` pasó a recursar en su rama
`idClase` (llama de nuevo a `<TipoGenericoOpcional>`), lo que habilita el
anidado en todos los contextos que ya usaban ese no terminal (atributos,
variables locales, `for`, `new`), sin tocarlos. El diamante es aparte, sólo
válido al instanciar: `<RestoNew>` pasa a usar `<TipoGenericoOpcionalNew>` /
`<DiamanteOTipo>` (interior vacío permitido), mientras que
`<TipoGenericoOpcional>` (el resto de contextos) sigue exigiendo un argumento
real — `Foo<> x;` sigue siendo error. La clase/interfaz sigue con un único
parámetro de tipo (`<GenericidadOpcional>` no cambió). El cierre `>>` de un
anidado tokeniza como dos `OP_MAYOR` sueltos (`estadoMayor()` sólo combina con
`=`), así que no hizo falta ningún cambio en el léxico — ver el hazard
correspondiente, ya resuelto, en "Hazards que todavía no violan LL(1)".

La **inicialización de atributos en la declaración** (`REQ-AS-011`) —
`<Atributo> ::= <Tipo> idMetVar ;` no admitía un `= <Expresion>` antes del
`;`— **ya está reflejada** en "Gramática Expandida (Logros)": `<RestoMiembro>`
suma la alternativa `<OperadorAsignacion> <ExpresionCompuesta> ;`. Sin
conflicto LL(1) que resolver (FIRST `{ = }` disjunto de `{ ; }` y `{ ( }`), así
que no hizo falta ninguna factorización nueva.

La **inicialización de arreglos en la construcción** (`REQ-AS-012`) —
`<CrearArreglo> ::= new <TipoBase> <DimensionesConTamanio>` no admitía un
inicializador entre llaves, y sólo se admite como parte de una expresión de
creación con `new`, nunca en otro lado— **ya está reflejada** en "Gramática
Expandida (Logros)": `<DimensionesNew>` reemplaza a `<DimensionesConTamanio>`
en `<RestoNew>`/`<RestoNewIdClase>` y, tras el primer `[`, decide con **un**
token más (`]` inmediato vs. una expresión) entre la forma de tamaño de
siempre y una forma nueva con todos los corchetes vacíos seguida de un
`<InicializadorArreglo>` (`{ 1, 2, 3 }`) obligatorio — no se pueden mezclar
tamaño e inicializador, igual que en Java. `<ValorArreglo>` admite anidar otro
inicializador, así que los multidimensionales (`new int[][]{{1,2},{3,4}}`)
salen sin costo adicional.

**Ya está reflejado** el **operador condicional ternario** (`REQ-AS-013`):
`condición ? expr : expr` se agrega como sufijo opcional de `<ExpresionCompuesta>`
(no de `<ExpresionBasica>`), para que la condición sea la cadena binaria completa
y el ternario quede con menos precedencia que cualquier operador binario, igual
que en Java — ver "Precedencia del operador ternario" para el detalle y un bug
real que apareció al integrarlo.

**Ya están reflejados** los **operadores unarios postfijos `++` y `--`**
(`REQ-AS-014`): `<PostfijoOpcional>` se agrega dentro de `<ExpresionBasica>`,
pegado a `<Operando>` (no como sufijo de `<ExpresionCompuesta>`), para que
aplique a cada término de la cadena binaria y no sólo al primero — ver
"Postfijo `++`/`--`" para el detalle, incluida la misma clase de bug de
`<TrasParenId>` que ya había aparecido con el ternario. `<OperadorUnario> ::=
+ | - | !` no cambia: sigue siendo sólo prefijo, el prefijo `++`/`--` queda
fuera de alcance de este requerimiento.

Estos ajustes (eliminar recursión izquierda, reescribiendo esas producciones
como repetición a derecha con un no terminal auxiliar tipo `<...Resto>`;
factorizar donde haga falta) se van a ir aplicando en revisiones futuras de
esta misma sección, dejando registro de qué se cambió y por qué en vez de
reemplazar la gramática de origen en silencio.

### Reglas que violan LL(1) — análisis de la gramática de partida

Para un descenso recursivo predictivo con un token de lookahead, cada no
terminal con más de una alternativa tiene que poder elegir rama mirando un solo
token, y ningún no terminal puede ser recursivo a izquierda. Repasando la
gramática de partida tal como está en el bloque de arriba (sin contar las
extensiones del Paso 5 —`REQ-AS-005..014`, de las cuales `REQ-AS-005` (lambda),
`REQ-AS-006` (variable local clásica), `REQ-AS-007` (visibilidad) y
`REQ-AS-009` (`for`) ya viven en "Gramática Expandida (Logros)"—, que suman sus
propios conflictos ya anotados en
las notas anteriores), estas son las reglas que lo impiden, agrupadas por tipo
de problema.

#### 1. Recursión a izquierda

Un no terminal `<X>` con una alternativa que arranca en `<X>`: `analizar<X>()`
se llamaría a sí mismo sin haber consumido ningún token y no terminaría.

| No terminal | Producción con recursión a izquierda |
| --- | --- |
| `<ListaArgsFormales>` | `<ListaArgsFormales> ::= <ListaArgsFormales> , <ArgFormal>` |
| `<ExpresionCompuesta>` | `<ExpresionCompuesta> ::= <ExpresionCompuesta> <OperadorBinario> <ExpresionBasica>` |
| `<Referencia>` | `<Referencia> ::= <Referencia> <VarEncadenada>` / `<Referencia> <MetodoEncadenado>` / `<Referencia> <AccesoArreglo>` (tres alternativas recursivas) |

Las tres son recursión a izquierda **inmediata**. No hay recursión a izquierda
indirecta: los ciclos entre `<Expresion>`, `<ExpresionCompuesta>`,
`<ExpresionBasica>`, `<Operando>`, `<Referencia>`, `<Primario>` y
`<ExpresionParentizada>` pasan siempre por el terminal `(` de
`<ExpresionParentizada> ::= ( <Expresion> )`, que consume token antes de volver
a entrar.

#### 2. Alternativas no factorizadas (conflicto FIRST/FIRST)

Dos o más alternativas del mismo no terminal cuyo FIRST comparte tokens: al ver
ese token como lookahead el parser no puede decidir qué rama tomar.

| No terminal | Alternativas en conflicto | Token(s) compartido(s) | Factorización |
| --- | --- | --- | --- |
| `<Miembro>` | `<Atributo>` vs `<Metodo>` | `boolean` `char` `int` `idClase` `idGen` (todo `FIRST(<Tipo>)`) | profunda: comparten el prefijo `<Tipo> idMetVar` y recién se distinguen en el token siguiente (`;` → atributo, `(` → método); `<Metodo>` además puede arrancar con `static` o `void`, que sí le son exclusivos |
| `<Primario>` | `<AccesoVar>` vs `<LlamadaMetodo>` | `idMetVar` | simple: prefijo `idMetVar`; `(` después → llamada, cualquier otra cosa → acceso a variable |
| `<Primario>` | `<CrearArreglo>` vs `<LlamadaConstructor>` | `new` (y, tras `new`, también `idClase` / `idGen`) | profunda: prefijo `new`; si sigue un tipo primitivo es `<CrearArreglo>`; si sigue `idClase` / `idGen` hay que factorizar otro nivel (`[` → arreglo, `(` o `<` → constructor) |
| `<Expresion>` | `<ExpresionCompuesta> <OperadorAsignacion> <ExpresionCompuesta>` vs `<ExpresionCompuesta>` | todo `FIRST(<ExpresionCompuesta>)` | simple: `<Expresion> ::= <ExpresionCompuesta> <RestoAsignacion>`, `<RestoAsignacion> ::= = <ExpresionCompuesta> \| ϵ` |
| `<If>` | `if ( <Expresion> ) <Sentencia>` vs la misma seguida de `else <Sentencia>` | el prefijo entero `if ( <Expresion> ) <Sentencia>` | simple: `<If> ::= if ( <Expresion> ) <Sentencia> <ElseOpcional>`, `<ElseOpcional> ::= else <Sentencia> \| ϵ` — y queda el `else` colgante (punto 4) |
| `<ListaExps>` | `<Expresion>` vs `<Expresion> , <ListaExps>` | todo `FIRST(<Expresion>)` | simple: `<ListaExps> ::= <Expresion> <RestoListaExps>`, `<RestoListaExps> ::= , <ListaExps> \| ϵ` |
| `<DimensionesConTamanio>` | `[ <Expresion> ]` vs `[ <Expresion> ] <DimensionesConTamanio>` | `[` | simple: extraer `[ <Expresion> ]` y dejar la repetición como cola opcional |
| `<ListaArgsFormales>` (después de sacarle la recursión a izquierda) | `<ArgFormal>` vs `<ArgFormal> , ...` | todo `FIRST(<ArgFormal>)` | simple: mismo patrón que `<ListaExps>`, cola `, <ArgFormal> ... \| ϵ` |

`<Sentencia>` —el otro candidato marcado para revisar— **no** tiene conflicto
FIRST/FIRST entre sus ramas estructurales: `;`, `var`, `return`, `if`, `while`,
`{` y `FIRST(<Expresion>)` son disjuntos entre sí. Su problema es otro (punto 3).

#### 3. Ambigüedad real (dos no terminales generan el mismo lenguaje)

| No terminal | Problema |
| --- | --- |
| `<Sentencia> ::= <Asignacion> ;` vs `<Sentencia> ::= <Llamada> ;` | `<Asignacion> ::= <Expresion>` y `<Llamada> ::= <Expresion>` tienen la parte derecha **idéntica**. No es falta de factorización: ninguna cantidad de lookahead las distingue porque derivan exactamente las mismas cadenas. Hay que unificarlas en una sola alternativa (`<Sentencia> ::= <Expresion> ;`) y dejar la distinción asignación / llamada para la etapa semántica (o resolverla según si la expresión contiene el `=` de asignación). |

#### 4. `else` colgante (dangling else)

Una vez factorizado `<If>` (punto 2), al ver `else` como lookahead
`analizarElseOpcional()` puede tanto tomar la rama `else <Sentencia>` como
reducir por `ϵ` (porque `else ∈ FOLLOW(<ElseOpcional>)` cuando el `if` está
anidado dentro de otro). Es la ambigüedad clásica; se resuelve por convención
**eligiendo siempre la rama `else`** (liga con el `if` más cercano). No hace
falta reescribir la gramática, solo fijar esa prioridad en el método.

#### 5. Hazards que todavía no violan LL(1) pero conviene registrar

- **`<` / `>` como operador relacional y como delimitador de genéricos
  (`REQ-AS-010`, ya resuelto).** En la gramática de partida no chocan porque
  `<TipoGenericoOpcional>` / `<GenericidadOpcional>` solo aparecen en contexto
  de tipo y `<OperadorBinario>` solo en contexto de expresión — eso sigue
  igual con genéricos anidados (`List<List<X>>`), porque anidar no cambia
  desde qué método se llama a `tipoGenericoOpcional()`. Y el cierre `>>`
  **no necesitó tratamiento especial**, a diferencia de lo que esta nota
  especulaba originalmente (la ambigüedad clásica de Java, donde `>>` es
  también el operador de shift): `AnalizadorLexicoImpl.estadoMayor()` sólo
  combina con `=` (para `>=`), así que `List<List<X>>` ya tokeniza como dos
  `OP_MAYOR` sueltos y cada nivel de anidamiento consume el suyo con su propio
  `match(OP_MAYOR)` en `tipoGenericoOpcional()`.
- **`(` sobrecargado.** `<ExpresionParentizada>`, `<ArgsActuales>`,
  `<ArgsFormales>` y —con `REQ-AS-005`— el arranque de una lambda `( ... ) ->`.
  Ya anotado en el "Detalle de REQ-AS-005".

### Cómo llevar la gramática a LL(1)

Plan de transformación sobre la gramática de partida. El orden es: **(0)** sanear
ambigüedades y duplicados, **(1)** eliminar la recursión a izquierda, **(2)**
factorizar a izquierda (simple y profunda), **(3)** verificar el FIRST/FOLLOW de
los `ϵ` nuevos, **(4)** fijar por convención los pocos conflictos que se dejan
sin reescribir. La recursión va primero porque al quitarla aparecen colas nuevas
que después hay que factorizar (`<Referencia>` es el caso). Cada reescritura
queda registrada acá; el bloque de gramática de arriba no se modifica en
silencio.

Técnicas usadas:

- **Eliminación de recursión a izquierda inmediata**: `<X> ::= <X> α | β` se
  reescribe como `<X> ::= β <X'>` y `<X'> ::= α <X'> | ϵ` (la repetición pasa a
  derecha con un no terminal auxiliar `<X'>`, que acá se nombra `<...Resto>`).
- **Factorización a izquierda simple**: `<X> ::= γ δ | γ ρ` se reescribe como
  `<X> ::= γ <X'>` y `<X'> ::= δ | ρ`.
- **Factorización a izquierda profunda**: si después de extraer `γ` las colas
  `δ` / `ρ` todavía comparten un prefijo, se repite el proceso un nivel más
  adentro.

#### Paso 0 — Ambigüedades y duplicados

`<Asignacion> ::= <Expresion>` y `<Llamada> ::= <Expresion>` tienen la parte
derecha idéntica (punto 3 del análisis). Se eliminan los dos no terminales y la
alternativa de `<Sentencia>` queda:

```
<Sentencia> ::= <Expresion> ;
```

Las demás alternativas de `<Sentencia>` no cambian. La distinción
asignación / llamada se hace en la etapa semántica (o mirando si la `<Expresion>`
tiene el `=` de asignación en la raíz).

#### Paso 1 — Eliminación de recursión a izquierda

**1.1 `<ListaArgsFormales>`** — `β = <ArgFormal>`, `α = , <ArgFormal>`:

```
<ListaArgsFormales>     ::= <ArgFormal> <ListaArgsFormalesResto>
<ListaArgsFormalesResto> ::= , <ArgFormal> <ListaArgsFormalesResto> | ϵ
```

**1.2 `<ExpresionCompuesta>`** — `β = <ExpresionBasica>`,
`α = <OperadorBinario> <ExpresionBasica>`:

```
<ExpresionCompuesta>     ::= <ExpresionBasica> <ExpresionCompuestaResto>
<ExpresionCompuestaResto> ::= <OperadorBinario> <ExpresionBasica> <ExpresionCompuestaResto> | ϵ
```

Esto deja todos los operadores binarios en una sola cadena asociativa a
izquierda y sin niveles de precedencia — que es exactamente cómo ya los trataba
la gramática de partida, así que no hay regresión. El ternario (`REQ-AS-013`,
ver "Precedencia del operador ternario") no necesitó esta escalera: alcanza con
colgarlo como sufijo de `<ExpresionCompuesta>` para que quede por debajo de
toda la cadena binaria.

**1.3 `<Referencia>`** — `β = <Primario>`, con tres `α`
(`<VarEncadenada>`, `<MetodoEncadenado>`, `<AccesoArreglo>`):

```
<Referencia>     ::= <Primario> <ReferenciaResto>
<ReferenciaResto> ::= <VarEncadenada> <ReferenciaResto>
                    | <MetodoEncadenado> <ReferenciaResto>
                    | <AccesoArreglo> <ReferenciaResto>
                    | ϵ
```

`<ReferenciaResto>` queda con un conflicto FIRST/FIRST nuevo: `<VarEncadenada>`
y `<MetodoEncadenado>` empiezan las dos con `.` (`. idMetVar` vs.
`. idMetVar <ArgsActuales>`). Se resuelve en el paso 2.

#### Paso 2 — Factorización a izquierda

**2.1 `<ReferenciaResto>`** (conflicto traído del paso 1.3) — prefijo común
`. idMetVar`; inlineando `<AccesoArreglo> ::= [ <Expresion> ]`:

```
<ReferenciaResto>      ::= . idMetVar <ArgsActualesOpcional> <ReferenciaResto>
                         | [ <Expresion> ] <ReferenciaResto>
                         | ϵ
<ArgsActualesOpcional> ::= <ArgsActuales> | ϵ
```

Decisión: tras `. idMetVar`, un `(` es método encadenado; cualquier otra cosa,
variable encadenada.

**2.2 `<Miembro>`** — factorización **profunda**. `public` es exclusivo de
constructor; `static` y `void` son exclusivos de método; `<Tipo> idMetVar` lo
comparten atributo y método y recién se separan en el token siguiente (`;` vs.
`(`):

```
<Miembro>     ::= public idClase <ArgsFormales> <Bloque>
                | static <TipoMetodo> idMetVar <ArgsFormales> <Bloque>
                | void idMetVar <ArgsFormales> <Bloque>
                | <Tipo> idMetVar <RestoMiembro>
<RestoMiembro> ::= ;
                 | <ArgsFormales> <Bloque>
```

(`<Atributo>`, `<Metodo>`, `<Constructor>` y `<ModificadorOpcional>` dejan de
usarse; `<TipoMetodo>` se conserva para la rama `static`.)

**2.3 `<Primario>` con `idMetVar`** — `<AccesoVar>` vs. `<LlamadaMetodo>`,
factorización simple:

```
<Primario>              ::= ... | idMetVar <ArgsActualesOpcional> | ...
```

(reusa `<ArgsActualesOpcional>` de 2.1: `(` → llamada, si no → acceso a
variable). `<AccesoVar>` y `<LlamadaMetodo>` se eliminan.

**2.4 `<Primario>` con `new`** — `<CrearArreglo>` vs. `<LlamadaConstructor>`,
factorización **profunda**:

```
<Primario>       ::= ... | new <RestoNew> | ...
<RestoNew>       ::= <TipoPrimitivo> <DimensionesConTamanio>
                   | idGen <DimensionesConTamanio>
                   | idClase <TipoGenericoOpcional> <RestoNewIdClase>
<RestoNewIdClase> ::= <DimensionesConTamanio>   
                    | <ArgsActuales>             
```

Primer nivel: se extrae `new`. Segundo nivel: si sigue tipo primitivo o `idGen`
es creación de arreglo; si sigue `idClase` todavía hay que mirar más allá del
`<TipoGenericoOpcional>` opcional — `[` → arreglo (`<DimensionesConTamanio>`),
`(` → constructor (`<ArgsActuales>`).

**2.5 `<Expresion>`** — prefijo común `<ExpresionCompuesta>`, factorización
simple:

```
<Expresion>      ::= <ExpresionCompuesta> <RestoAsignacion>
<RestoAsignacion> ::= <OperadorAsignacion> <ExpresionCompuesta> | ϵ
```

**2.6 `<If>`** — prefijo común `if ( <Expresion> ) <Sentencia>`, factorización
simple:

```
<If>          ::= if ( <Expresion> ) <Sentencia> <ElseOpcional>
<ElseOpcional> ::= else <Sentencia> | ϵ
```

`<ElseOpcional>` queda con el conflicto del `else` colgante (paso 4).

**2.7 `<ListaExps>`** — `<Expresion>` vs. `<Expresion> , <ListaExps>`,
factorización simple:

```
<ListaExps>      ::= <Expresion> <RestoListaExps>
<RestoListaExps> ::= , <Expresion> <RestoListaExps> | ϵ
```

**2.8 `<DimensionesConTamanio>`** — prefijo común `[ <Expresion> ]`,
factorización simple:

```
<DimensionesConTamanio>    ::= [ <Expresion> ] <DimensionesConTamanioOpc>
<DimensionesConTamanioOpc> ::= [ <Expresion> ] <DimensionesConTamanioOpc> | ϵ
```

`<DimensionesConTamanioOpc>` queda con un conflicto FIRST/FOLLOW sobre `[`
(paso 4), porque `[` también puede abrir un `<AccesoArreglo>` en
`<ReferenciaResto>`.

#### Paso 3 — Verificación de los `ϵ` nuevos

Para cada auxiliar anulable hay que confirmar `FIRST(parte no vacía) ∩ FOLLOW = ∅`:

| Auxiliar | FIRST de la parte no vacía | ¿Conflicto con su FOLLOW? |
| --- | --- | --- |
| `<ListaArgsFormalesResto>` | `,` | no |
| `<ExpresionCompuestaResto>` | operadores binarios (`\|\|` `&&` `==` `!=` `<` `>` `<=` `>=` `+` `-` `*` `/` `%`) | no — su FOLLOW es `{ = ) ; ] , }`, disjunto (`=` ≠ `==`) |
| `<ReferenciaResto>` | `.` `[` | no — su FOLLOW no tiene `.` ni `[` suelto |
| `<ArgsActualesOpcional>` | `(` | no |
| `<RestoAsignacion>` | `=` | no |
| `<RestoListaExps>` | `,` | no |
| `<RestoMiembro>` | `;` `(` | no (no es anulable: siempre produce algo) |
| `<ElseOpcional>` | `else` | **sí, deliberado** → paso 4 |
| `<DimensionesConTamanioOpc>` | `[` | **sí, deliberado** → paso 4 |

#### Paso 4 — Conflictos que se dejan y se resuelven por convención

No se reescribe la gramática para estos; se fija la prioridad en el método
correspondiente:

- **`else` colgante** (`<ElseOpcional>`): si el lookahead es `else`, tomar
  siempre `else <Sentencia>` (el `else` liga con el `if` más cercano).
- **`[` tras dimensiones** (`<DimensionesConTamanioOpc>`): si el lookahead es
  `[`, seguir consumiendo dimensiones. Es decir, `new int[2][3]` es creación de
  un arreglo 2D, no `(new int[2])[3]`. El acceso a arreglo con `[` recién se
  toma en `<ReferenciaResto>`, cuando `<DimensionesConTamanio>` ya no puede
  seguir.

#### Paso 5 — Al incorporar `REQ-AS-007..014`

Cada extensión pendiente vuelve a pasar por los pasos 1–3 sobre los no
terminales que toca. **Ya están incorporadas** siete: la lambda
(`REQ-AS-005`), con su gramática factorizada en "Gramática Expandida (Logros)"
y el conflicto del prefijo `(` en "Factorización del prefijo `(`"; la variable
local clásica (`REQ-AS-006`), con el conflicto del prefijo `idClase` en
"Factorización de `<Sentencia>`"; la visibilidad de miembros (`REQ-AS-007`),
con la factorización profunda sobre `idClase` (constructor vs. tipo clase) en
"Factorización de `<Miembro>`"; el `for` (`REQ-AS-009`), con el conflicto
clásico-vs-for-each (prefijo `<Tipo> idMetVar` compartido) y el mismo choque de
`idClase` en "Factorización de `<For>`"; los genéricos anidados y la notación
diamante (`REQ-AS-010`), que no necesitaron ninguna factorización nueva (sólo
`<InstanciadoOParametrico>` recursando y una variante ϵ-decidible para el
diamante en `<RestoNew>`) — el `<` / `>` / `>>` que se anotaba como
"diagnóstico propio" resultó no ser un problema real, ver el hazard
correspondiente; los inicializadores de atributo (`REQ-AS-011`), una
alternativa más de `<RestoMiembro>` con FIRST disjunto de las otras dos (sin
factorización); y los inicializadores de arreglo entre llaves (`REQ-AS-012`),
con un conflicto chico y puntual: tras el primer `[` de una creación con
`new`, un `]` inmediato compromete a la forma con inicializador (corchetes
vacíos + `{ ... }` obligatorio) en vez de la forma de tamaño de siempre —
mismo patrón de "un token más decide" que el resto del documento, sin
necesitar factorización profunda; el ternario (`REQ-AS-013`), colgado de
`<ExpresionCompuesta>` como sufijo opcional (ver "Precedencia del operador
ternario") — sin conflicto que factorizar, un solo token (`?`) decide; y el
postfijo `++`/`--` (`REQ-AS-014`), colgado de `<ExpresionBasica>` (ver
"Postfijo `++`/`--`") — también sin conflicto, un solo token (`OP_INCREMENTO`
u `OP_DECREMENTO`) decide.

### Gramática LL(1) resultante

Gramática de partida con los pasos 0 a 2 ya aplicados (eliminación de la
ambigüedad `<Asignacion>`/`<Llamada>`, eliminación de recursión a izquierda y
factorización a izquierda). Misma notación BNF; no terminal inicial `<Inicial>`.

Alcance y límites de esta versión:

- Este bloque **no** incorpora las extensiones del **Paso 5**. La lambda
  (`REQ-AS-005`), la variable local clásica (`REQ-AS-006`), la visibilidad de
  miembros (`REQ-AS-007`), el `for` (`REQ-AS-009`), los genéricos anidados y
  la notación diamante (`REQ-AS-010`), los inicializadores de atributo
  (`REQ-AS-011`), los inicializadores de arreglo entre llaves (`REQ-AS-012`),
  el ternario (`REQ-AS-013`) y el postfijo `++`/`--` (`REQ-AS-014`) ya están
  integrados, pero en la sección aparte "Gramática Expandida (Logros)", no
  acá. El **Paso 5 queda completo**: no hay más extensiones pendientes de
  reflejar en la gramática.
- Quedan dos conflictos **deliberados** que no se reescriben y se resuelven por
  convención en el método correspondiente (Paso 4): `<ElseOpcional>` sobre
  `else` (liga con el `if` más cercano) y `<DimensionesConTamanioOpc>` sobre `[`
  (sigue consumiendo dimensiones: `new int[2][3]` es un arreglo 2D). En el resto
  de los no terminales anulables el FIRST de la parte no vacía es disjunto de su
  FOLLOW (tabla del Paso 3).
- No terminales que **desaparecen** respecto de la gramática de partida:
  `<Asignacion>`, `<Llamada>` (Paso 0); `<Atributo>`, `<Metodo>`,
  `<Constructor>`, `<ModificadorOpcional>` (Paso 2.2); `<AccesoVar>`,
  `<LlamadaMetodo>` (Paso 2.3); `<CrearArreglo>`, `<LlamadaConstructor>`
  (Paso 2.4); `<VarEncadenada>`, `<MetodoEncadenado>`, `<AccesoArreglo>`
  (inlineados en `<ReferenciaResto>`, Paso 1.3 / 2.1).
- No terminales **nuevos** (todos auxiliares de repetición a derecha o de
  factorización): `<RestoMiembro>`, `<ListaArgsFormalesResto>`, `<ElseOpcional>`,
  `<RestoAsignacion>`, `<ExpresionCompuestaResto>`, `<ReferenciaResto>`,
  `<ArgsActualesOpcional>`, `<RestoNew>`, `<RestoNewIdClase>`,
  `<DimensionesConTamanioOpc>`, `<RestoListaExps>`.

```
<Inicial> ::= <ListaClases> eof

<ListaClases> ::= <Clase> <ListaClases> | <Interfaz> <ListaClases> | ϵ

<Clase> ::= class idClase <GenericidadOpcional> <HerenciaOpcional> { <ListaMiembros> }

<Interfaz> ::= interface idClase <GenericidadOpcional> <ExtensionOpcional> { <ListaMetodosInterfaz> }

<GenericidadOpcional> ::= < idGen > | ϵ

<HerenciaOpcional> ::= extends <TipoReferencia> | implements <TipoReferencia> | ϵ

<ExtensionOpcional> ::= extends <TipoReferencia> | ϵ

<ListaMiembros> ::= <Miembro> <ListaMiembros> | ϵ

<ListaMetodosInterfaz> ::= <MetodoInterfaz> <ListaMetodosInterfaz> | ϵ

<Miembro> ::= public idClase <ArgsFormales> <Bloque>
<Miembro> ::= static <TipoMetodo> idMetVar <ArgsFormales> <Bloque>
<Miembro> ::= void idMetVar <ArgsFormales> <Bloque>
<Miembro> ::= <Tipo> idMetVar <RestoMiembro>

<RestoMiembro> ::= ; | <ArgsFormales> <Bloque>

<MetodoInterfaz> ::= <TipoMetodo> idMetVar <ArgsFormales> ;

<TipoMetodo> ::= <Tipo> | void

<Tipo> ::= <TipoBase> <DimensionesOpcionales>

<TipoBase> ::= <TipoPrimitivo> | <TipoReferencia> | idGen

<DimensionesOpcionales> ::= [ ] <DimensionesOpcionales> | ϵ

<TipoReferencia> ::= idClase <TipoGenericoOpcional>

<TipoPrimitivo> ::= boolean | char | int

<TipoGenericoOpcional> ::= < <InstanciadoOParametrico> > | ϵ

<InstanciadoOParametrico> ::= idGen | idClase

<ArgsFormales> ::= ( <ListaArgsFormalesOpcional> )

<ListaArgsFormalesOpcional> ::= <ListaArgsFormales> | ϵ

<ListaArgsFormales> ::= <ArgFormal> <ListaArgsFormalesResto>

<ListaArgsFormalesResto> ::= , <ArgFormal> <ListaArgsFormalesResto> | ϵ

<ArgFormal> ::= <Tipo> idMetVar

<Bloque> ::= { <ListaSentencias> }

<ListaSentencias> ::= <Sentencia> <ListaSentencias> | ϵ

<Sentencia> ::= ;
<Sentencia> ::= <Expresion> ;
<Sentencia> ::= <VarLocal> ;
<Sentencia> ::= <Return> ;
<Sentencia> ::= <If>
<Sentencia> ::= <While>
<Sentencia> ::= <Bloque>

<VarLocal> ::= var idMetVar = <ExpresionCompuesta>

<Return> ::= return <ExpresionOpcional>

<ExpresionOpcional> ::= <Expresion> | ϵ

<If> ::= if ( <Expresion> ) <Sentencia> <ElseOpcional>

<ElseOpcional> ::= else <Sentencia> | ϵ

<While> ::= while ( <Expresion> ) <Sentencia>

<Expresion> ::= <ExpresionCompuesta> <RestoAsignacion>

<RestoAsignacion> ::= <OperadorAsignacion> <ExpresionCompuesta> | ϵ

<OperadorAsignacion> ::= =

<ExpresionCompuesta> ::= <ExpresionBasica> <ExpresionCompuestaResto>

<ExpresionCompuestaResto> ::= <OperadorBinario> <ExpresionBasica> <ExpresionCompuestaResto> | ϵ

<OperadorBinario> ::= || | && | == | != | < | > | <= | >= | + | - | * | / | %

<ExpresionBasica> ::= <OperadorUnario> <Operando> | <Operando>

<OperadorUnario> ::= + | - | !

<Operando> ::= <Primitivo> | <Referencia>

<Primitivo> ::= true | false | intLiteral | charLiteral | null

<Referencia> ::= <Primario> <ReferenciaResto>

<ReferenciaResto> ::= . idMetVar <ArgsActualesOpcional> <ReferenciaResto>
<ReferenciaResto> ::= [ <Expresion> ] <ReferenciaResto>
<ReferenciaResto> ::= ϵ

<ArgsActualesOpcional> ::= <ArgsActuales> | ϵ

<Primario> ::= this
<Primario> ::= stringLiteral
<Primario> ::= idMetVar <ArgsActualesOpcional>
<Primario> ::= new <RestoNew>
<Primario> ::= <LlamadaMetodoEstatico>
<Primario> ::= <ExpresionParentizada>

<RestoNew> ::= <TipoPrimitivo> <DimensionesConTamanio>
<RestoNew> ::= idGen <DimensionesConTamanio>
<RestoNew> ::= idClase <TipoGenericoOpcional> <RestoNewIdClase>

<RestoNewIdClase> ::= <DimensionesConTamanio> | <ArgsActuales>

<ExpresionParentizada> ::= ( <Expresion> )

<LlamadaMetodoEstatico> ::= idClase . idMetVar <ArgsActuales>

<DimensionesConTamanio> ::= [ <Expresion> ] <DimensionesConTamanioOpc>

<DimensionesConTamanioOpc> ::= [ <Expresion> ] <DimensionesConTamanioOpc> | ϵ

<ArgsActuales> ::= ( <ListaExpsOpcional> )

<ListaExpsOpcional> ::= <ListaExps> | ϵ

<ListaExps> ::= <Expresion> <RestoListaExps>

<RestoListaExps> ::= , <Expresion> <RestoListaExps> | ϵ
```

Notas de las decisiones que quedan tomadas en el método, no en la gramática:

- `<ReferenciaResto>` (Paso 2.1): tras `. idMetVar`, si el lookahead es `(` es
  método encadenado (`<ArgsActualesOpcional>` toma `<ArgsActuales>`); cualquier
  otra cosa, variable encadenada (`<ArgsActualesOpcional>` reduce por `ϵ`).
- `<Primario>` con `idMetVar` (Paso 2.3): mismo criterio — `(` siguiente →
  llamada a método; si no → acceso a variable. Reusa `<ArgsActualesOpcional>`.
- `<RestoNewIdClase>` (Paso 2.4): tras `new idClase <TipoGenericoOpcional>`, un
  `[` abre creación de arreglo y un `(` una llamada a constructor.
- `<ElseOpcional>`: con lookahead `else`, tomar siempre `else <Sentencia>`.
- `<DimensionesConTamanioOpc>`: con lookahead `[`, seguir consumiendo
  dimensiones; el `[` de acceso a arreglo recién se considera en
  `<ReferenciaResto>`, cuando `<DimensionesConTamanio>` ya no puede continuar.
- `<Sentencia> ::= <Expresion> ;` (Paso 0): la distinción asignación / llamada
  se difiere a la etapa semántica (o se mira si la `<Expresion>` tiene el `=`
  de asignación en la raíz).

## Relación con el Analizador Léxico

A diferencia del Módulo Principal de la etapa 1 (que le pedía al léxico
`startAnalizar()` y recibía todos los tokens de una sola pasada vía
`ResultadoLexicoListener`), el sintáctico consume tokens **bajo demanda**, de a
uno, con el modelo *pull*:

```java
public interface AnalizadorLexico {
    void startAnalizar() throws IOException; // modo etapa 1: barrido completo, push
    Token nextToken();                        // modo etapa 2: un token por llamada, pull
}
```

Contrato que el sintáctico asume de `nextToken()`:

- Cada llamada devuelve el **próximo token válido** del programa fuente. El
  sintáctico nunca ve un carácter suelto ni un lexema a medio armar: eso es
  responsabilidad exclusiva del léxico, igual que hoy.
- Los **errores léxicos no interrumpen la secuencia**: si el léxico encuentra
  un carácter inválido o un lexema mal formado, lo reporta por su cuenta (mismo
  mecanismo de hoy, o el que se decida al implementar `nextToken()`) y sigue
  devolviendo tokens a partir de ahí, tal como `startAnalizar()` no corta la
  ejecución ante el primer error léxico. El sintáctico no tiene por qué
  enterarse de que hubo un error léxico previo: solo ve tokens.
- Al llegar al final del archivo, `nextToken()` devuelve un `Token` de tipo
  `TokenType.EOF` (lexema `"$"`, mismo criterio que ya usa `startAnalizar()`).
  Llamadas posteriores a esa deben seguir devolviendo EOF en vez de lanzar una
  excepción o devolver `null`, para no obligar al sintáctico a un chequeo
  especial de "ya se acabó, no llamar de nuevo".

Nota de implementación: `AnalizadorLexicoImpl` implementa los dos modos sobre el
mismo autómata. `startAnalizar()` recorre el archivo completo empujando tokens al
listener (etapa 1). `nextToken()` reusa `estadoInicial()` token por token: en la
primera llamada ceba `caracterActual` (flag `iniciado`, que `startAnalizar()`
también setea), después vuelve a llamar a `estadoInicial()` hasta que se arme un
token o se llegue a EOF, salteando blancos y comentarios. Los errores léxicos se
siguen informando por `listener.onError(...)` y no interrumpen la secuencia; al
llegar a EOF devuelve el token `EOF` en esa llamada y en todas las siguientes.
Como la interfaz de consumo del sintáctico es *pull* y sin checked exceptions,
una `IOException` del `SourceManager` se re-lanza envuelta en
`UncheckedIOException`.

## Gramática Expandida (Logros).

```
<Inicial> ::= <ListaClases> eof

<ListaClases> ::= <Clase> <ListaClases> | <Interfaz> <ListaClases> | ϵ

<Clase> ::= class idClase <GenericidadOpcional> <HerenciaOpcional> { <ListaMiembros> }

<Interfaz> ::= interface idClase <GenericidadOpcional> <ExtensionOpcional> { <ListaMetodosInterfaz> }

<GenericidadOpcional> ::= < idGen > | ϵ

<HerenciaOpcional> ::= extends <TipoReferencia> | implements <TipoReferencia> | ϵ

<ExtensionOpcional> ::= extends <TipoReferencia> | ϵ

<ListaMiembros> ::= <Miembro> <ListaMiembros> | ϵ

<ListaMetodosInterfaz> ::= <MetodoInterfaz> <ListaMetodosInterfaz> | ϵ

# REQ-AS-007 · visibilidad: <Visibilidad> es prefijo común de todo miembro.
# public deja de marcar al constructor => hay que factorizar la rama idClase
# entre constructor (idClase "(") y tipo clase (idClase "<" / "[" / idMetVar).
<Miembro> ::= <Visibilidad> <CuerpoMiembro>

<Visibilidad> ::= public | private | ϵ

<CuerpoMiembro> ::= static <TipoMetodo> idMetVar <ArgsFormales> <Bloque>
<CuerpoMiembro> ::= void idMetVar <ArgsFormales> <Bloque>
<CuerpoMiembro> ::= <TipoPrimitivo> <DimensionesOpcionales> idMetVar <RestoMiembro>
<CuerpoMiembro> ::= idGen <DimensionesOpcionales> idMetVar <RestoMiembro>
<CuerpoMiembro> ::= idClase <TrasIdClaseMiembro>

<TrasIdClaseMiembro> ::= <ArgsFormales> <Bloque>
<TrasIdClaseMiembro> ::= <TipoGenericoOpcional> <DimensionesOpcionales> idMetVar <RestoMiembro>

# REQ-AS-011 · inicializador de atributo: tercera rama de <RestoMiembro>, FIRST
# { = } disjunto de { ; } y { ( } — sin conflicto LL(1) que factorizar.
<RestoMiembro> ::= ; | <ArgsFormales> <Bloque> | <OperadorAsignacion> <ExpresionCompuesta> ;

<MetodoInterfaz> ::= <TipoMetodo> idMetVar <ArgsFormales> ;

<TipoMetodo> ::= <Tipo> | void

<Tipo> ::= <TipoBase> <DimensionesOpcionales>

<TipoBase> ::= <TipoPrimitivo> | <TipoReferencia> | idGen

<DimensionesOpcionales> ::= [ ] <DimensionesOpcionales> | ϵ

<TipoReferencia> ::= idClase <TipoGenericoOpcional>

<TipoPrimitivo> ::= boolean | char | int

<TipoGenericoOpcional> ::= < <InstanciadoOParametrico> > | ϵ

# REQ-AS-010 · genéricos anidados: la rama idClase recursa en <TipoGenericoOpcional>
# (antes era idClase a secas). Habilita Caja<Lista<Item>> en todo lo que ya usaba
# <TipoGenericoOpcional> (no hace falta tocar esos no terminales). idGen no
# anida: un parámetro de tipo no puede llevar su propio argumento.
<InstanciadoOParametrico> ::= idGen | idClase <TipoGenericoOpcional>

<ArgsFormales> ::= ( <ListaArgsFormalesOpcional> )

<ListaArgsFormalesOpcional> ::= <ListaArgsFormales> | ϵ

<ListaArgsFormales> ::= <ArgFormal> <ListaArgsFormalesResto>

<ListaArgsFormalesResto> ::= , <ArgFormal> <ListaArgsFormalesResto> | ϵ

<ArgFormal> ::= <Tipo> idMetVar

<Bloque> ::= { <ListaSentencias> }

<ListaSentencias> ::= <Sentencia> <ListaSentencias> | ϵ

<Sentencia> ::= ;
<Sentencia> ::= <VarLocal> ;
<Sentencia> ::= <TipoPrimitivo> <RestoDeclLocal>
<Sentencia> ::= idGen <RestoDeclLocal>
<Sentencia> ::= idClase <SentIdClase>
<Sentencia> ::= <Return> ;
<Sentencia> ::= <If>
<Sentencia> ::= <While>
<Sentencia> ::= <For>
<Sentencia> ::= <Bloque>
<Sentencia> ::= <Expresion> ;      # sólo si el lookahead ∈ FIRST(<Expresion>) \ { idClase }

<VarLocal> ::= var idMetVar = <ExpresionCompuesta>

# --- REQ-AS-006 · variable local clásica (sin var) -----------------------------
# int x;  /  int x, y, z = 10;  /  T g;  /  Foo x;  /  Foo<Bar> x = new Foo<Bar>();
# Un único "= <ExpresionCompuesta>" al final vale para toda la lista de nombres;
# a qué variables les aplica el valor es semántico. El prefijo idClase se comparte
# con la expresión (idClase . idMetVar (...)) y se factoriza en <SentIdClase>
# mirando el token que sigue (ver "Factorización de <Sentencia>").

<SentIdClase> ::= <TipoGenericoOpcional> <RestoDeclLocal>
<SentIdClase> ::= . idMetVar <ArgsActuales> <ReferenciaResto> <ExpresionCompuestaResto> <RestoAsignacion> ;

<RestoDeclLocal> ::= idMetVar <MasIdsLocal> <InitLocalOpc> ;

<MasIdsLocal> ::= , idMetVar <MasIdsLocal> | ϵ

# --- REQ-AS-009 · sentencia "for" (clásica y for-each) -------------------------
# Clásica: for ( init ; cond ; act ) Sentencia — las tres secciones son
# opcionales, como en Java (for(;;)). For-each: for ( Tipo idMetVar : expr )
# Sentencia. El cuerpo es <Sentencia> (no <Bloque>), igual que <If>/<While>.
# Cada sección admite una sola sentencia/expresión, nunca listas por coma
# (REQ-AS-009 lo pide explícitamente). Ambas formas comparten el prefijo
# "<Tipo> idMetVar" y se factorizan igual que en el resto del documento
# (ver "Factorización de <For>").

<For> ::= for ( <ClausulasFor> ) <Sentencia>

<ClausulasFor> ::= ; <CondFor> ; <ActFor>
<ClausulasFor> ::= <TipoPrimitivo> idMetVar <TrasIdForTipo>
<ClausulasFor> ::= idGen idMetVar <TrasIdForTipo>
<ClausulasFor> ::= idClase <TrasIdClaseFor>
<ClausulasFor> ::= <Expresion> ; <CondFor> ; <ActFor>   # sólo con FIRST(<Expresion>) \ { idClase }

<TrasIdForTipo> ::= : <Expresion>
<TrasIdForTipo> ::= <InitLocalOpc> ; <CondFor> ; <ActFor>

<TrasIdClaseFor> ::= <TipoGenericoOpcional> idMetVar <TrasIdForTipo>
<TrasIdClaseFor> ::= . idMetVar <ArgsActuales> <ReferenciaResto> <ExpresionCompuestaResto> <RestoAsignacion> ; <CondFor> ; <ActFor>

<CondFor> ::= <Expresion> | ϵ
<ActFor>  ::= <Expresion> | ϵ

<InitLocalOpc> ::= <OperadorAsignacion> <ExpresionCompuesta> | ϵ

<Return> ::= return <ExpresionOpcional>

<ExpresionOpcional> ::= <Expresion> | ϵ

<If> ::= if ( <Expresion> ) <Sentencia> <ElseOpcional>

<ElseOpcional> ::= else <Sentencia> | ϵ

<While> ::= while ( <Expresion> ) <Sentencia>

<Expresion> ::= <ExpresionCompuesta> <RestoAsignacion>

<RestoAsignacion> ::= <OperadorAsignacion> <ExpresionCompuesta> | ϵ

<OperadorAsignacion> ::= =

<ExpresionCompuesta> ::= <ExpresionBasica> <ExpresionCompuestaResto> <TernarioOpcional>

<ExpresionCompuestaResto> ::= <OperadorBinario> <ExpresionBasica> <ExpresionCompuestaResto> | ϵ

<OperadorBinario> ::= || | && | == | != | < | > | <= | >= | + | - | * | / | %

# REQ-AS-013 · ternario: se cuelga de <ExpresionCompuesta>, no de <ExpresionBasica>,
# para que la condición sea la cadena binaria YA completa (menos precedencia que
# cualquier operador binario, como en Java). <valorFalse> es <ExpresionCompuesta>,
# que ya termina en su propio <TernarioOpcional> -- un ternario anidado ahí
# (a ? b : c ? d : e) sale asociado a derecha sin repetir nada acá. Ver
# "Precedencia del operador ternario" para el detalle y un bug real que apareció
# al cablearlo en <TrasParenId>.
<TernarioOpcional> ::= ? <ExpresionCompuesta> : <ExpresionCompuesta> | ϵ

# REQ-AS-014 · postfijo ++/--: se cuelga de <ExpresionBasica>, no de
# <ExpresionCompuesta>, para que aplique a CADA término de la cadena binaria
# (expresionBasica() se llama una vez por término, tanto acá como desde
# <ExpresionCompuestaResto>), no sólo al primero. <OperadorUnario> sigue siendo
# sólo prefijo (+ | - | !); el prefijo ++/-- queda fuera de alcance.
# <PostfijoOpcional> es recursiva para admitir encadenados ("a++--"), igual
# que la gramática real de Java (que sólo lo rechaza en el chequeo de tipos,
# no en el parser) — ver "Postfijo `++`/`--`" para el detalle, y el mismo bug
# de <TrasParenId> que ya había aparecido con el ternario.
<ExpresionBasica> ::= <OperadorUnario> <Operando> <PostfijoOpcional> | <Operando> <PostfijoOpcional>

<PostfijoOpcional> ::= ++ <PostfijoOpcional> | -- <PostfijoOpcional> | ϵ

<OperadorUnario> ::= + | - | !

# Forma CONCEPTUAL de <Operando> (legible, no LL(1)): dice sólo que la lambda es
# una alternativa más. NO es la que se parsea: <Referencia> y <Lambda> comparten
# "(" y "idMetVar" en su FIRST. La definición operativa es la factorizada de más
# abajo (tras las reglas de <Lambda>), que REEMPLAZA a esta línea.
<Operando> ::= <Primitivo> | <Referencia> | <Lambda>

<Primitivo> ::= true | false | intLiteral | charLiteral | null

<Referencia> ::= <Primario> <ReferenciaResto>

<ReferenciaResto> ::= . idMetVar <ArgsActualesOpcional> <ReferenciaResto>
<ReferenciaResto> ::= [ <Expresion> ] <ReferenciaResto>
<ReferenciaResto> ::= ϵ

<ArgsActualesOpcional> ::= <ArgsActuales> | ϵ

<Primario> ::= this
<Primario> ::= stringLiteral
<Primario> ::= idMetVar <ArgsActualesOpcional>
<Primario> ::= new <RestoNew>
<Primario> ::= <LlamadaMetodoEstatico>
<Primario> ::= <ExpresionParentizada>

<RestoNew> ::= <TipoPrimitivo> <DimensionesNew>
<RestoNew> ::= idGen <DimensionesNew>
# REQ-AS-010 · notación diamante: sólo válida al instanciar (new Foo<>()), nunca
# en una declaración de tipo. <TipoGenericoOpcionalNew> admite el interior vacío;
# el resto de contextos sigue usando <TipoGenericoOpcional> sin cambios (exige un
# argumento real: Foo<> x; sigue siendo error).
<RestoNew> ::= idClase <TipoGenericoOpcionalNew> <RestoNewIdClase>

<TipoGenericoOpcionalNew> ::= < <DiamanteOTipo> > | ϵ

<DiamanteOTipo> ::= <InstanciadoOParametrico> | ϵ    # ϵ = notación diamante "<>"

<RestoNewIdClase> ::= <DimensionesNew> | <ArgsActuales>

<ExpresionParentizada> ::= ( <Expresion> )

<LlamadaMetodoEstatico> ::= idClase . idMetVar <ArgsActuales>

# REQ-AS-012 · inicialización de arreglos con llaves, solo al construir con
# "new" (new int[]{1,2,3}). "]" inmediato tras el "[" ⇒ TODOS los corchetes de
# esta creación van vacíos y viene un inicializador obligatorio (no se puede
# mezclar tamaño e inicializador, igual que en Java); una <Expresion> ahí ⇒
# sigue la forma de tamaño de siempre, sin cambios.
<DimensionesNew> ::= [ <TrasCorcheteNew>

<TrasCorcheteNew> ::= ] <MasCorchetesVaciosNew> <InicializadorArreglo>
<TrasCorcheteNew> ::= <Expresion> ] <DimensionesConTamanioOpc>

<MasCorchetesVaciosNew> ::= [ ] <MasCorchetesVaciosNew> | ϵ

<InicializadorArreglo> ::= { <ListaValoresArregloOpcional> }

<ListaValoresArregloOpcional> ::= <ListaValoresArreglo> | ϵ    # "{ }" vacío también es válido

<ListaValoresArreglo> ::= <ValorArreglo> <RestoValoresArreglo>

<RestoValoresArreglo> ::= , <ValorArreglo> <RestoValoresArreglo> | ϵ

# <ValorArreglo> anidado habilita arreglos multidimensionales con inicializador
# (new int[][]{{1,2},{3,4}}) gratis, sin costo de factorización adicional.
<ValorArreglo> ::= <ExpresionCompuesta> | <InicializadorArreglo>

# <DimensionesConTamanioOpc> sigue igual: la reusa <TrasCorcheteNew> para el
# resto de corchetes con tamaño tras el primero. <DimensionesConTamanio> (el
# no terminal que bundleaba "[ <Expresion> ] <DimensionesConTamanioOpc>") queda
# reemplazada por <DimensionesNew> en los dos lugares donde se usaba.
<DimensionesConTamanioOpc> ::= [ <Expresion> ] <DimensionesConTamanioOpc> | ϵ

<ArgsActuales> ::= ( <ListaExpsOpcional> )

<ListaExpsOpcional> ::= <ListaExps> | ϵ

<ListaExps> ::= <Expresion> <RestoListaExps>

<RestoListaExps> ::= , <Expresion> <RestoListaExps> | ϵ

# --- REQ-AS-005 · expresión lambda -------------------------------------------
# Forma: <ParamsLambda> -> <Expresion>. Los parámetros no llevan tipo (lista de
# idMetVar); el cuerpo es UNA expresión, nunca "{ ... }". Con cero parámetros el
# "( )" es obligatorio; con un solo parámetro los paréntesis son opcionales.
# La lambda es una alternativa más de <Operando> (ver la línea de <Operando>).

<Lambda> ::= <ParamsLambda> -> <Expresion>

<ParamsLambda> ::= ( <ParamsLambdaEntreParen> ) | idMetVar

<ParamsLambdaEntreParen> ::= <ListaIdLambda> | ϵ

<ListaIdLambda> ::= idMetVar <RestoListaIdLambda>

<RestoListaIdLambda> ::= , idMetVar <RestoListaIdLambda> | ϵ

# <Lambda> deja <Operando> con conflicto FIRST/FIRST: comparte "(" con
# <ExpresionParentizada> y "idMetVar" con <Primario> ::= idMetVar ... . La forma
# factorizada que vuelve LL(1) a <Operando> inlinea <Referencia>, <Primario> y
# <ExpresionParentizada> y reparte sus arranques con estos productos nuevos.
# >>> Esta es la definición OPERATIVA de <Operando> (la que implementa el parser):
#     REEMPLAZA a "<Operando> ::= <Primitivo> | <Referencia> | <Lambda>" de arriba.

<Operando> ::= <Primitivo>
<Operando> ::= this <ReferenciaResto>
<Operando> ::= stringLiteral <ReferenciaResto>
<Operando> ::= new <RestoNew> <ReferenciaResto>
<Operando> ::= <LlamadaMetodoEstatico> <ReferenciaResto>
<Operando> ::= idMetVar <TrasId>
<Operando> ::= ( <TrasParen>

# <TrasId>: tras "idMetVar", "->" => lambda de 1 parámetro sin paréntesis (x -> e);
#           cualquier otra cosa => referencia normal (x | x(a) | x.f ...).
<TrasId> ::= -> <Expresion>
<TrasId> ::= <ArgsActualesOpcional> <ReferenciaResto>

# <TrasParen>: tras "(", ")" => lambda de 0 parámetros ( () -> e );
#              "idMetVar" => todavía podría ser parámetro, decide <TrasParenId>;
#              cualquier otro arranque de <Expresion> => nunca es lambda (un
#              parámetro siempre es "idMetVar" o "()"), se parsea <Expresion>,
#              se exige ")" y se sigue con <ReferenciaResto> sin más chequeos.
<TrasParen> ::= ) -> <Expresion>
<TrasParen> ::= idMetVar <TrasParenId>
<TrasParen> ::= <Expresion> ) <ReferenciaResto>

# <TrasParenId>: tras "( idMetVar", "," => lista de parámetros => lambda
#                de >=2 parámetros ( ( x , y , ... ) -> e );
#                ")" => cierra con un solo idMetVar, caso ambiguo real,
#                decide <DecidirTrasCierre>;
#                cualquier otro token (operador, ".", "[", "(", "=", "?", "++", "--",
#                o el "->" de una lambda anidada) => el idMetVar no era parámetro,
#                era el comienzo de una expresión más grande: se completa reusando
#                <TrasId>/<PostfijoOpcional>/<ExpresionCompuestaResto>/<TernarioOpcional>/
#                <RestoAsignacion> (sin duplicar la jerarquía de precedencia) y, al
#                cerrar con ")", ya no se vuelve a ofrecer "->". Acá aparecieron los
#                dos bugs reales de esta lista, mismo patrón los dos: esta rama
#                reconstruye <Expresion> a mano y al agregar una extensión nueva se
#                olvidó sumarla acá. REQ-AS-013: sin <TernarioOpcional>,
#                "(c > d ? c : d)" fallaba (ver "Precedencia del operador ternario").
#                REQ-AS-014: sin <PostfijoOpcional>, "(a++)" fallaba (ver "Postfijo
#                `++`/`--`").
<TrasParenId> ::= , <ListaIdLambda> ) -> <Expresion>
<TrasParenId> ::= ) <DecidirTrasCierre>
<TrasParenId> ::= <TrasId> <PostfijoOpcional> <ExpresionCompuestaResto> <TernarioOpcional> <RestoAsignacion> ) <ReferenciaResto>

# <DecidirTrasCierre>: tras "( idMetVar )", "->" => era ( x ) -> e => lambda de
#                      1 parámetro; cualquier otra cosa => expresión
#                      parentizada normal ( x ), y se sigue con <ReferenciaResto>.
<DecidirTrasCierre> ::= -> <Expresion>
<DecidirTrasCierre> ::= <ReferenciaResto>
```

### Factorización del prefijo `(` — lambda vs. expresión parentizada

**El conflicto.** Al agregar `<Lambda>` como alternativa de `<Operando>`, hay
dos no terminales alcanzables desde `<Operando>` que arrancan con `(`:

- `<ExpresionParentizada> ::= ( <Expresion> )` (vía `<Referencia> → <Primario>`)
- `<Lambda> ::= <ParamsLambda> -> <Expresion>` con `<ParamsLambda> ::= ( ... )`

Con el `(` como único token de lookahead el parser no puede decidir cuál de las
dos tomar: es un conflicto FIRST/FIRST. Y **no se arregla mirando `k` tokens
fijos**, porque los casos sólo se distinguen *después* del `)` que cierra, y
adentro del paréntesis puede haber una expresión arbitrariamente larga:

```
( a + b )            → expresión parentizada
( a )                → expresión parentizada  (la expresión es sólo 'a')
( a ) -> a + 1       → lambda de 1 parámetro
( a , b ) -> a + b   → lambda de 2 parámetros
( ) -> 0             → lambda de 0 parámetros
```

`( a )` y `( a ) ->` difieren recién en el token que viene *después* del `)`.
No hay un `k` que alcance ⇒ la gramática con `<ExpresionParentizada>` y
`<Lambda>` como alternativas separadas no es LL(k).

**La factorización.** En vez de decidir al ver el `(`, se consume el `(` y se
posterga la decisión al primer token que sí desambigüe (esto es factorización a
izquierda: reconocer el prefijo común una sola vez y ramificar después). Tras
el `(`:

1. Si el siguiente token es `)` → sólo puede ser `( ) ->` ⇒ **lambda de 0
   parámetros** (`<TrasParen> ::= ) -> <Expresion>`).
2. Si el siguiente token es `idMetVar` → **todavía podría ser un parámetro**
   (único requisito sintáctico de un parámetro: ser un `idMetVar` suelto o una
   lista de ellos separados por coma), así que hay que seguir mirando
   (`<TrasParenId>`):
   - `,` → era una lista de parámetros ⇒ **lambda**; se siguen leyendo
     `, idMetVar` hasta el `)` y después se exige `->`.
   - `)` → se consume y se mira **un** token más (`<DecidirTrasCierre>`):
     - `->` → era `( x ) -> ...` ⇒ **lambda de 1 parámetro**.
     - cualquier otra cosa → era `( x )` ⇒ **expresión parentizada** (la
       variable `x` sola); se sigue con `<ReferenciaResto>` (`.m()`, `[i]`,
       etc.).
   - cualquier otro token (operador, `.`, `[`, `(`, `=`, o incluso un `->` de
     una lambda anidada, vía `<TrasId>`) → el `idMetVar` **no** era un
     parámetro, era apenas el comienzo de una expresión más grande. Ya se sabe
     con certeza que esto no puede ser una lista de parámetros (ningún
     operador ni continuación de referencia es válido ahí), así que se termina
     de parsear como expresión normal (reusando `<TrasId>`,
     `<ExpresionCompuestaResto>` y `<RestoAsignacion>` para no duplicar la
     jerarquía de precedencia) y, al llegar al `)` de cierre, **no se vuelve a
     ofrecer `->`**: sólo puede ser expresión parentizada.
3. Si el siguiente token es cualquier otro arranque válido de `<Expresion>`
   (literal, `this`, `stringLiteral`, `new`, `idClase`, `(` anidado, operador
   unario) → **nunca puede ser lambda** (un parámetro siempre es `idMetVar` o
   la lista vacía `()`), así que se parsea `<Expresion>` normal, se exige `)` y
   se sigue con `<ReferenciaResto>` — sin ningún chequeo de `->` posterior.

Con eso cada no terminal nuevo (`<TrasParen>`, `<TrasParenId>`,
`<DecidirTrasCierre>`) decide con **un** token de lookahead y sus alternativas
tienen FIRST disjuntos (`)` / `idMetVar` / resto de `FIRST(<Expresion>)` en
`<TrasParen>`; `,` / `)` / resto en `<TrasParenId>`; `->` / resto en
`<DecidirTrasCierre>`), o sea que la gramática es LL(1).

**El costo.** A diferencia de una primera factorización más simple (parsear
siempre una `<Expresion>` genérica tras el `(` y decidir recién con el token
posterior al `)` de cierre), acá el costo no es sobre-aceptación sino
duplicación potencial: la rama de `<TrasParenId>` que descarta la lista de
parámetros no puede llamar de nuevo a `<Expresion>` desde cero (perdería el
`idMetVar` ya consumido), así que reconstruye el resto de la jerarquía de
precedencia llamando explícitamente a `<TrasId>` (cierra el `<Operando>` que
arrancó en `idMetVar`), `<ExpresionCompuestaResto>` (sigue con más operadores
binarios) y `<RestoAsignacion>` (admite un `=` final, para no perder casos como
`(x = y)`). Son los mismos tres métodos que ya usa el resto del parser para
construir `<Expresion>`, así que no hay lógica nueva de precedencia — sólo un
punto de entrada distinto a la cadena habitual.

**Pendiente — revisión futura (resuelto).** El rechazo de `(a + b) -> e` (y
`(a = b) -> e`, `(f(x)) -> e`, `(5) -> e`, …) estaba diferido a una etapa
semántica que todavía no existe en el proyecto. Se evaluaron tres tratamientos:

1. **Diferir a semántico.** La gramática factorizada acepta cualquier
   `<Expresion>` en posición de parámetro; semántico comprueba que sea un
   único `idMetVar`. Cero cambios de gramática, pero requiere una etapa que no
   existe.
2. **Lookback en el método.** En `trasParen()`, cuando tras `( <Expresion> )`
   viene `->`, exigir que la `<Expresion>` ya parseada sea un `idMetVar`
   suelto; si no, error sintáctico. Sin producciones nuevas, pero deja de ser
   LL(1) estricto en ese punto (decide mirando la estructura ya parseada, no
   sólo el token actual).
3. **Split de `<TrasParen>` por `idMetVar`.** Bifurcar según si el primer token
   tras `(` es `idMetVar` (posible parámetro) o no, con un `<TrasParenId>` que
   ramifica en `,` / `)` / resto-de-expresión. Rechaza directamente en el
   parseo, sigue siendo LL(1) con un token de lookahead en cada punto, y sólo
   suma dos no terminales (`<TrasParenId>`, `<DecidirTrasCierre>`) reusando el
   resto de la jerarquía de `<Expresion>` ya existente.

**Decisión: se adopta la opción 3** (arriba, ya integrada en "La
factorización" y en `<TrasParenId>`/`<DecidirTrasCierre>` de la gramática
factorizada más abajo). Queda implementada en `trasParen()` / `trasParenId()`
/ `decidirTrasCierre()` de `AnalizadorSintacticoImpl`.

El prefijo `idMetVar` (lambda `x -> e` sin paréntesis vs. acceso a variable) es
el caso fácil: alcanza **un** token de lookahead — si tras el `idMetVar` viene
`->` es lambda, si no es una referencia (`<TrasId>`), mismo patrón que ya usa
`idMetVar <ArgsActualesOpcional>` (`(` → llamada, si no → variable).

**Token nuevo en el léxico.** La flecha `->` no se leía como un token propio
(daba `OP_MENOS` seguido de `OP_MAYOR`). **Ya está agregado**: `ARROW("op->")`
en `TokenType` y una rama en `estadoMenos()` del autómata que, por *maximal
munch*, arma `ARROW` al ver `-` seguido de `>` (misma forma que `--` / `->` /
`-`). Sirve para los dos modos del léxico (`startAnalizar` push y `nextToken`
pull) porque ambos pasan por `estadoInicial()`.

### Factorización de `<Sentencia>` — declaración local clásica vs. expresión con `idClase`

**El conflicto.** Al sumar la variable local clásica (`REQ-AS-006`),
`<Sentencia>` gana alternativas que arrancan con `FIRST(<TipoBase>) = { boolean,
char, int, idGen, idClase }`. De esos, `boolean` / `char` / `int` / `idGen` no
están en `FIRST(<Expresion>)`, así que `int x, y, z = 10;` no choca con nada.

`idClase` **sí** está en `FIRST(<Expresion>)`, pero por un único camino:
`<LlamadaMetodoEstatico> ::= idClase . idMetVar <ArgsActuales>`. O sea:

- en posición de **expresión**, tras `idClase` **siempre** viene `.`
  (`Fabrica.crear();`);
- en posición de **tipo** (declaración), tras `idClase` viene `<` (genérico) o
  `idMetVar` (el nombre de la variable): `Foo x;`, `Foo<Bar> x = ...;`.

**La factorización.** Misma idea que `<TrasId>` en la lambda: se consume
`idClase` y **un** token más desambigua, sin lookahead extra.

```
<Sentencia> ::= ... | idClase <SentIdClase> | ... | <Expresion> ;

<SentIdClase> ::= <TipoGenericoOpcional> <RestoDeclLocal>          # decl: Foo x;  Foo<Bar> x = ...;
              |  . idMetVar <ArgsActuales> <ReferenciaResto> <ExpresionCompuestaResto> <RestoAsignacion> ;   # expr: Foo.m()...;
```

- Lookahead `.` ⇒ era una expresión; la rama reconstruye la cola de
  `<Expresion>` que arrancaba en `idClase . idMetVar <ArgsActuales>` (los mismos
  no terminales que recorrería `expresion()`), así no se pierde lenguaje.
- Cualquier otro token ⇒ declaración local.
- FIRST disjuntos: rama-1 = `{ <, idMetVar }` (vía `<TipoGenericoOpcional>` y
  `<RestoDeclLocal>` anulables hasta `idMetVar`) vs. rama-2 = `{ . }`.
- La alternativa `<Sentencia> ::= <Expresion> ;` sigue existiendo, pero el
  método `sentencia()` la prueba **después** de la rama `idClase`, así que en la
  práctica sólo entra con `FIRST(<Expresion>) \ { idClase }`.

**Init compartido.** `<RestoDeclLocal> ::= idMetVar <MasIdsLocal> <InitLocalOpc> ;`
admite un único `= <ExpresionCompuesta>` al final para toda la lista de nombres
(`int x, y, z = 10;`). Que el valor "les llegue a las tres" es una regla
**semántica**; el sintáctico sólo valida la forma (igual criterio que la
posición de parámetro en la lambda).

**Sin token nuevo.** No hace falta nada en el léxico: `<TipoPrimitivo>`,
`idGen`, `idClase`, `,`, `=` y `;` ya existen.

### Factorización de `<Miembro>` — visibilidad opcional vs. prefijo `idClase` del constructor

**El conflicto.** Al sumar la visibilidad (`REQ-AS-007`),
`<Visibilidad> ::= public | private | ϵ` se antepone a todo `<Miembro>`. Por sí
sola no rompe nada: es anulable, con `FIRST = { public, private }` disjunto de
`FOLLOW(<Visibilidad>) = FIRST(<CuerpoMiembro>) = { static, void, boolean, char,
int, idGen, idClase }`. El problema es indirecto. En la versión del Paso 2.2,
`public` era **el** token que identificaba al constructor
(`<Miembro> ::= public idClase <ArgsFormales> <Bloque>`). Al pasar `public` a ser
una visibilidad más, el constructor queda como `idClase <ArgsFormales> <Bloque>`
y su prefijo `idClase` choca con el de un atributo o método cuyo tipo es una
clase (`<Tipo>` puede empezar con `idClase` vía `<TipoReferencia>`):

- `Foo(...) { }`      → constructor
- `Foo bar(...) { }`  → método que devuelve `Foo`
- `Foo bar ;`         → atributo de tipo `Foo`
- `Foo<X> bar ;`      → atributo, tipo genérico

**La factorización.** Profunda sobre `idClase`, misma técnica que
`<RestoNewIdClase>` (Paso 2.4) y `<SentIdClase>` ("Factorización de
`<Sentencia>`"): se consume `idClase` y **un** token más desambigua, sin
lookahead extra.

```
<Miembro> ::= <Visibilidad> <CuerpoMiembro>

<Visibilidad> ::= public | private | ϵ

<CuerpoMiembro> ::= static <TipoMetodo> idMetVar <ArgsFormales> <Bloque>
               |  void idMetVar <ArgsFormales> <Bloque>
               |  <TipoPrimitivo> <DimensionesOpcionales> idMetVar <RestoMiembro>
               |  idGen <DimensionesOpcionales> idMetVar <RestoMiembro>
               |  idClase <TrasIdClaseMiembro>

<TrasIdClaseMiembro> ::= <ArgsFormales> <Bloque>                                                # "(" ⇒ constructor
                     |  <TipoGenericoOpcional> <DimensionesOpcionales> idMetVar <RestoMiembro>   # "<" / "[" / idMetVar ⇒ tipo clase

<RestoMiembro> ::= ; | <ArgsFormales> <Bloque> | <OperadorAsignacion> <ExpresionCompuesta> ;
                                          # ";" atributo sin inicializar, "(" método,
                                          # "=" atributo con inicializador (REQ-AS-011)
```

- Hay que **desplegar `<Tipo>`** (en el Paso 2.2 estaba sin desplegar, en
  `<Miembro> ::= <Tipo> idMetVar <RestoMiembro>`): `<TipoPrimitivo>` e `idGen` no
  chocan con constructor (un constructor se llama como la clase, siempre
  `idClase`), así que van directos; sólo la rama `idClase` necesita el nivel
  extra.
- `<TrasIdClaseMiembro>`: FIRST rama-1 = `{ ( }` (por `<ArgsFormales>`); FIRST
  rama-2 = `{ <, [, idMetVar }` (por `<TipoGenericoOpcional>` y
  `<DimensionesOpcionales>` anulables hasta `idMetVar`). Disjuntos.
- `<CuerpoMiembro>`: `{ static }` / `{ void }` / `{ boolean, char, int }` /
  `{ idGen }` / `{ idClase }`. Disjuntos.
- `<Visibilidad>` anulable: `FIRST(no vacío) = { public, private }` y
  `FOLLOW(<Visibilidad>) = FIRST(<CuerpoMiembro>) = { static, void, boolean,
  char, int, idGen, idClase }`; la intersección es vacía, así que el `ϵ`
  (visibilidad implícita) no genera conflicto.
- Qué visibilidad implica omitir `<Visibilidad>` y si `private` es legal en cada
  contexto (p. ej. no en interfaz) es **semántico**; el sintáctico sólo valida
  la forma.

**Token nuevo en el léxico.** `public` ya es palabra clave (`PR_PUBLIC`);
`private` no. Hay que agregar `PR_PRIVATE` a `TokenType` y a
`TablaPalabrasClave` (mismo tipo de cambio que `ARROW` para la lambda).

**Interfaces.** `<MetodoInterfaz> ::= <TipoMetodo> idMetVar <ArgsFormales> ;` no
cambia: los métodos de interfaz quedan implícitamente `public`, sin sintaxis de
visibilidad.

### Factorización de `<For>` — clásico vs. for-each, y prefijo `idClase`

**El conflicto.** `REQ-AS-009` pide dos formas de `for`:

- Clásica: `for ( <Tipo> idMetVar [ = expr ] ; cond ; act ) Sentencia`
- For-each: `for ( <Tipo> idMetVar : expr ) Sentencia`

Ambas arrancan con el mismo prefijo `<Tipo> idMetVar` y sólo se distinguen por
el token que viene *después* del nombre de la variable (`:` vs. `=`/`;`) — un
conflicto FIRST/FIRST idéntico en forma a los que ya resolvió este documento
(`<TrasId>` de la lambda, `<SentIdClase>`, `<TrasIdClaseMiembro>`). Además, la
inicialización del `for` clásico también puede ser una **expresión** en vez de
una declaración (`for (i = 0; ...)`, `for (Fabrica.reset(); ...)`), lo que
reintroduce el mismo choque de `idClase` (¿tipo o llamada estática?) que ya
aparece en `<Sentencia>`/`<SentIdClase>` y en `<Miembro>`/`<TrasIdClaseMiembro>`.

**La factorización.** Se consume el prefijo común y se decide con **un** token
más en cada punto, igual que en los casos anteriores:

```
<For> ::= for ( <ClausulasFor> ) <Sentencia>

<ClausulasFor> ::= ; <CondFor> ; <ActFor>                            # sin inicialización
                 |  <TipoPrimitivo> idMetVar <TrasIdForTipo>
                 |  idGen idMetVar <TrasIdForTipo>
                 |  idClase <TrasIdClaseFor>
                 |  <Expresion> ; <CondFor> ; <ActFor>               # init. es una expresión

<TrasIdForTipo> ::= : <Expresion>                                    # for-each
                  |  <InitLocalOpc> ; <CondFor> ; <ActFor>           # for clásico con declaración

<TrasIdClaseFor> ::= <TipoGenericoOpcional> idMetVar <TrasIdForTipo>                                                    # era tipo clase
                   |  . idMetVar <ArgsActuales> <ReferenciaResto> <ExpresionCompuestaResto> <RestoAsignacion> ; <CondFor> ; <ActFor>  # era llamada estática

<CondFor> ::= <Expresion> | ϵ
<ActFor>  ::= <Expresion> | ϵ
```

- `<ClausulasFor>`: FIRST disjuntos — `{ ; }` / `{ boolean, char, int }` /
  `{ idGen }` / `{ idClase }` / resto de `FIRST(<Expresion>)` (sin `idClase`,
  que ya lo toma la rama anterior — mismo criterio que `<Sentencia>` con
  `<SentIdClase>`).
- `<TrasIdForTipo>`: `{ : }` vs. `{ =, ; }` (por `<InitLocalOpc>` anulable).
  Disjuntos.
- `<TrasIdClaseFor>`: `{ . }` vs. `{ <, idMetVar }` (por `<TipoGenericoOpcional>`
  anulable). Disjuntos — misma forma que `<TrasIdClaseMiembro>`.
- `<CondFor>`/`<ActFor>` anulables: no generan conflicto porque lo que sigue en
  cada caso (`;` o `)`) no está en `FIRST(<Expresion>)`.

**Decisiones de diseño** (no impuestas por el requerimiento, confirmadas antes
de implementar):
- Las tres secciones son **opcionales**, igual que en Java (`for (;;)` es
  válido) — de ahí que `<CondFor>`/`<ActFor>` acepten `ϵ` y que `<ClausulasFor>`
  tenga una rama para "sin inicialización".
- El **cuerpo es `<Sentencia>`**, no `<Bloque>`: admite tanto una sentencia
  simple sin llaves como un bloque, igual que `<If>`/`<While>` — no hay motivo
  para que `for` sea más estricto que esos dos.
- El **for-each no admite `var`** (sólo tipo explícito) por ahora; se puede
  sumar después como una alternativa más de `<ClausulasFor>`
  (`var idMetVar : <Expresion>`) sin romper nada, porque `var` es palabra clave
  propia y no comparte prefijo con nada de lo de arriba.
- **Alcance**: igual que `<RestoDeclLocal>` hoy, no se admiten arreglos en la
  variable declarada en el `for` (`<DimensionesOpcionales>` queda afuera),
  consistente con que `REQ-AS-006` tampoco los cubre todavía.
- La `<Actualizacion>` de Java en rigor sólo admite expresiones-sentencia
  (asignación, `++`/`--`, llamada), no cualquier `<Expresion>`; se optó por
  `<Expresion>` sin restricción porque el proyecto ya acepta esa misma
  amplitud en `<Sentencia> ::= <Expresion> ;` (p. ej. `(1+2)*3;` es una
  sentencia válida hoy) — restringir sólo acá sería inconsistente, y no es un
  chequeo sintáctico sino de estilo/semántica.

**Reuso.** Nada nuevo salvo la factorización en sí: `<Expresion>`,
`<InitLocalOpc>`, `<TipoGenericoOpcional>`, y la misma reconstrucción de cola
de `<SentIdClase>` (`<ArgsActuales> <ReferenciaResto> <ExpresionCompuestaResto>
<RestoAsignacion>`) para la rama de llamada estática como inicialización.

**Token nuevo en el léxico.** `for` no era palabra clave. Se agregó `PR_FOR` a
`TokenType` y a `TablaPalabrasClave` (mismo tipo de cambio que `PR_PRIVATE`
para la visibilidad). El `:` de for-each ya existía como `DOS_PUNTOS` — no hizo
falta tocar el autómata del léxico.

### Precedencia del operador ternario (REQ-AS-013)

**La decisión de diseño.** Una primera propuesta colgaba el ternario de
`<ExpresionBasica>` (al mismo nivel que `<OperadorUnario> <Operando>`), con
`<Ternario> ::= ? <valorTrue> : <valorFalse>`. Eso le da al `?:` **más**
precedencia que cualquier operador binario — lo opuesto a Java, donde el
ternario tiene de las precedencias más bajas (sólo por encima de la
asignación). Ejemplo donde se nota: `1 + flag ? a : b` debería leerse
`(1 + flag) ? a : b`; colgado de `<ExpresionBasica>`, el `?:` se ata sólo al
último operando de la cadena (`flag`) y da `1 + (flag ? a : b)`.

La corrección fue mover el enganche a `<ExpresionCompuesta>` — el nivel que ya
encadena toda la cadena de operadores binarios (`<ExpresionCompuestaResto>`) —
como sufijo opcional:

```
<ExpresionCompuesta> ::= <ExpresionBasica> <ExpresionCompuestaResto> <TernarioOpcional>
<TernarioOpcional>   ::= ? <ExpresionCompuesta> : <ExpresionCompuesta> | ϵ
```

Para cuando `<TernarioOpcional>` mira el `?`, la condición (`<ExpresionBasica>
<ExpresionCompuestaResto>`) ya está completamente consumida, así que el
ternario queda por debajo de todos los binarios, igual que en Java:
`1 + flag ? a : b` → `(1+flag) ? a : b`; `flag ? 1 : 2 + 3` → `flag ? 1 : (2+3)`.

**Asociatividad a derecha sin recursión explícita.** El `<valorFalse>` es
`<ExpresionCompuesta>`, que ya termina en su propio `<TernarioOpcional>` — no
hace falta escribir `<TernarioOpcional>` de nuevo al final de la producción
para soportar `a ? b : c ? d : e`. Al llegar al `c`, la llamada recursiva a
`<ExpresionCompuesta>` (la del `<valorFalse>` externo) prueba su propio
`<TernarioOpcional>`, ve el `?` que sigue y arma `c ? d : e` como una unidad,
dando `a ? b : (c ? d : e)` (asociativo a derecha, como Java). Mismo mecanismo
para un ternario anidado en el `<valorTrue>` (`a ? b?c:d : e`): cada `?` se
resuelve con su `:` más cercano por el propio anidado de llamadas, sin
ambigüedad LL(1) (basta un token de lookahead en cada punto de decisión).

**Bug real encontrado al integrarlo.** `<TrasParenId>` (factorización del
prefijo `(` de "Factorización del prefijo `(`") tiene una rama que reconstruye
`<Expresion>` a mano para resolver el conflicto lambda-vs-paréntesis, sin poder
reusar `expresionCompuesta()` directamente:
`<TrasId> <ExpresionCompuestaResto> <RestoAsignacion> ) <ReferenciaResto>`. Al
sumar el ternario se pisó agregarle `<TernarioOpcional>` ahí, así que
`(c > d ? c : d)` fallaba con `se esperaba "parC" y se encontró interrogacion`
— el paréntesis esperaba cerrar antes de llegar al `?`. Lo destapó
`sintCorrecto18.java` (ternario anidado con un paréntesis explícito). Quedó
corregido con `<TernarioOpcional>` sumado ahí (forma final, con el postfijo de
`REQ-AS-014` incluido, en "Postfijo `++`/`--`" más abajo) — mismo patrón que en
`<ExpresionCompuesta>`, ternario antes que asignación.

**Token nuevo en el léxico.** `?` no existía. Se agregó `TokenType.INTERROGACION`
y se reconoce como símbolo simple en `estadoInicial()` (no combina con ningún
otro carácter, a diferencia de `<`/`>`/`=`/`!`/`&`/`|`/`+`/`-`/`/`).

### Postfijo `++`/`--` (REQ-AS-014)

**Dónde engancha.** Mismo tipo de decisión que el ternario, pero en la
dirección opuesta: acá el enganche va en `<ExpresionBasica>` (no en
`<ExpresionCompuesta>`), porque el postfijo es una propiedad *por operando*,
no de la expresión completa — igual que `<OperadorUnario>`, que ya vive en ese
mismo nivel:

```
<ExpresionBasica>  ::= <OperadorUnario> <Operando> <PostfijoOpcional>
                    |  <Operando> <PostfijoOpcional>
<PostfijoOpcional> ::= ++ <PostfijoOpcional> | -- <PostfijoOpcional> | ϵ
```

Una primera propuesta lo colgaba una sola vez de `<ExpresionCompuesta>`
(`<ExpresionBasica> <PostfijoOpcional> <ExpresionCompuestaResto>`), pensando
en evitar "repetir" el chequeo. El problema: `<ExpresionCompuestaResto>`
vuelve a llamar a `<ExpresionBasica>` para cada término siguiente de la cadena
binaria, y esa llamada recursiva no pasaba por ese `<PostfijoOpcional>` — sólo
el primer término de la cadena quedaba cubierto. `a + x++;` fallaba: se
parsea `a`, no hay postfijo ahí; `<ExpresionCompuestaResto>` consume `+ x`
como segundo término sin volver a chequear postfijo; el `++` queda colgando y
truena contra el `;` esperado.

La corrección real no es "menos repetición" sino "en qué no terminal vive la
regla": `<ExpresionBasica>` ya es la unidad de "un operando con signo
opcional" que se llama una vez por término (acá mismo y desde
`<ExpresionCompuestaResto>`), así que ponerle el postfijo ahí lo cubre en
todos los términos sin escribir el chequeo dos veces — al revés de lo que
parecía, colgarlo de `<ExpresionCompuesta>` es lo que hubiera necesitado
duplicar `<PostfijoOpcional>` (una vez ahí y otra dentro de
`<ExpresionCompuestaResto>`) para cubrir todos los términos correctamente.

**Bug real encontrado al integrarlo — mismo punto ciego que el ternario.**
`<TrasParenId>` reconstruye `<Operando>`/`<Expresion>` a mano para el
conflicto lambda-vs-paréntesis (`<TrasId> <ExpresionCompuestaResto>
<TernarioOpcional> <RestoAsignacion> ) <ReferenciaResto>`), sin pasar por
`expresionBasica()`. Se le había olvidado sumar `<PostfijoOpcional>` ahí
también, así que `(a++)` fallaba (`se esperaba "parC" y se encontró op++`).
Quedó corregido como `<TrasId> <PostfijoOpcional> <ExpresionCompuestaResto>
<TernarioOpcional> <RestoAsignacion> ) <ReferenciaResto>` — postfijo pegado a
`<TrasId>` (que es la cola de `<Operando> ::= idMetVar <TrasId>`), en el mismo
lugar relativo que en `<ExpresionBasica>`.

**Fuera de alcance.** `<OperadorUnario>` sigue siendo sólo `+ | - | !`: el
prefijo `++x`/`--x` no forma parte de `REQ-AS-014` (que pide explícitamente
"postfijos") y sigue siendo error sintáctico.

**Postfijos encadenados (`a++--`) sí se aceptan — a propósito, para calzar con
Java real.** Una primera versión de `<PostfijoOpcional>` no era recursiva (un
solo `++`/`--` por operando) y rechazaba `a++--` como error sintáctico,
justificado como "el resultado de `a++` no es una variable, no puede llevar
otro postfijo". Se verificó contra `javac` real y esa justificación era
incorrecta: la gramática de Java **sí** es recursiva ahí
(`PostIncrementExpression ::= PostfixExpression ++`, y `PostfixExpression`
puede ser a su vez otro `PostIncrementExpression`/`PostDecrementExpression`),
así que `javac` acepta `a++--` al parsear y recién lo rechaza en el chequeo de
**tipos** (`error: unexpected type — required: variable, found: value`), no
en el parser — la distinción "esto es una variable asignable, esto es sólo un
valor" es semántica, no sintáctica. Cortarlo en el sintáctico hubiera sido
una restricción propia sin base en Java ni en el resto del proyecto (que ya
difiere esa misma distinción a semántica en otros lados: `1++`, `1 = 2;`), así
que `<PostfijoOpcional>` quedó recursiva y `a++--` es un caso positivo
(`sintCorrecto20.java`).

**Token nuevo en el léxico.** Ninguno: `OP_INCREMENTO` (`++`) y
`OP_DECREMENTO` (`--`) ya existían en `TokenType` y ya los emitía
`AnalizadorLexicoImpl` (`estadoMas()`/`estadoMenos()`), quedaron sin usar en
el sintáctico hasta ahora.

## Estrategia de análisis: descenso recursivo predictivo (LL(1))

El sintáctico usa **descenso recursivo predictivo con un solo token de
lookahead**, que es lo que ya insinúa el esqueleto existente (`tokenActual`,
`match()`):

- **Un método privado por no terminal** de la gramática, siguiendo la misma
  idea que el léxico ("un método por estado del autómata"): acá es "un método
  por no terminal de la gramática". Ej.: si `SIntaxis.md` define
  `<Clase> ::= pr_class idClase LlaveA <ListaMiembro> LlaveC`, existe un método
  `analizarClase()` que llama a `match(PR_CLASS)`, `match(ID_CLASE)`,
  `match(LLAVE_A)`, `analizarListaMiembro()`, `match(LLAVE_C)`, en ese orden.
- **Lookahead de un token** (`tokenActual`): en todo momento el parser tiene en
  memoria el próximo token sin consumir. Sirve para decidir, en las
  producciones con alternativas (`<X> ::= α | β`), cuál rama tomar mirando
  `tokenActual.getTipo()` contra el conjunto FIRST de cada alternativa — sin
  necesitar retroceder ni pedir más de un token por adelantado. Esto es lo que
  hace que la gramática deba ser LL(1): cada decisión tiene que poder tomarse
  con ese único token.
- **Producciones anulables (`ϵ`)**: cuando un no terminal tiene una alternativa
  `ϵ` (`<X> ::= α | ϵ`), el método la representa con un `else` explícito de
  cuerpo vacío — solo el comentario `// ϵ — no hace nada` — en vez de dejar el
  `if` sin `else`. Hoy es un no-op y no cambia el análisis, pero deja la rama
  `ϵ` visible y uniforme como punto de extensión para cuando cada método tenga
  que devolver o construir algo (AST, acciones semánticas). Los no terminales
  **sin** `ϵ` conservan en su lugar el `else` que llama a `error(...)`: si
  ninguna alternativa tiene el lookahead en su FIRST, es un error sintáctico.
- **`match(TokenType esperado)`**: primitiva común a todos los métodos de no
  terminal.
  - Si `tokenActual.getTipo() == esperado`, consume: pide el próximo token al
    léxico (`tokenActual = lexico.nextToken()`) y continúa.
  - Si no coincide, es un error sintáctico (ver más abajo): el token que había
    no es el que la gramática esperaba en ese punto de la derivación.
- **`start()`**: punto de entrada. Pide el primer token
  (`tokenActual = lexico.nextToken()`), invoca el método del símbolo inicial de
  la gramática (el no terminal raíz que defina `SIntaxis.md`, p. ej.
  `<Programa>`) y, al volver de esa llamada, exige que lo único que quede sea
  EOF. Si sobran tokens después de haber cerrado la derivación del símbolo
  inicial, es un error ("se esperaba fin de archivo, se encontró X") — señal de
  algo mal cerrado (una llave de más, código suelto después de la última
  clase, etc.).

```
start()
  tokenActual := lexico.nextToken()
  analizar<SímboloInicial>()
  match(EOF)

match(esperado)
  si tokenActual.tipo == esperado:
      tokenActual := lexico.nextToken()
  si no:
      reportar error sintáctico
```

## Manejo de errores sintácticos

Se busca el mismo criterio que en el léxico: **no cortar en el primer error**
si se puede evitar, para poder reportar varios errores en una sola corrida.

- **Detección**: un error sintáctico ocurre cuando `tokenActual` no pertenece
  al conjunto de tokens que la producción actual puede aceptar en ese punto
  (falla un `match()`, o ninguna alternativa de una producción con `|` tiene a
  `tokenActual` en su FIRST).
- **Reporte**: análogo al formato de error léxico ya usado (`ErrorLexico` /
  REQ-MP-08 de la etapa 1: línea, motivo, línea fuente), adaptado a lo que un
  `Token` puede ofrecer:
  - línea (`token.getLinea()`),
  - qué se encontró (`token.getTipo().getNombre()` + `token.getLexema()`),
  - qué se esperaba (el/los `TokenType` que el punto de la gramática admitía).

  Limitación a tener en cuenta: `Token` no guarda columna (solo línea), a
  diferencia de `ErrorLexico` que sí la tiene porque se arma carácter a
  carácter. Por eso el error sintáctico no puede dibujar un `^` apuntando a una
  columna exacta como hace el léxico — como mucho, señala la línea y el lexema
  del token ofensivo. Si más adelante hace falta esa precisión, `Token`
  tendría que empezar a llevar columna también; queda como decisión abierta,
  no asumida acá.
- **Recuperación (modo pánico)**: tras reportar un error, en vez de abortar,
  el parser descarta tokens (`tokenActual = lexico.nextToken()`) hasta
  encontrar uno de un **conjunto de sincronización** razonable para el punto
  donde se cortó (típicamente algo del FOLLOW del no terminal en curso, o un
  delimitador estructural fuerte como `puntoYComa` o `llaveC`), y continúa el
  análisis desde ahí. Esto permite seguir buscando más errores en el resto del
  archivo, al costo de que un error temprano puede arrastrar "ecos" (errores
  en cascada) si la sincronización no fue la correcta — un trade-off inherente
  al modo pánico, no un bug a resolver por completo.
- Igual que en el léxico, si no hubo errores sintácticos se puede reusar la
  idea de una salida tipo `[SinErrores]`; si los hubo, se listan todos al
  final (o a medida que se detectan, si se decide informar en streaming como
  hace hoy `ResultadoLexicoListener`).

## Piezas que va a necesitar el diseño (a definir junto con la implementación)

- `ErrorSintactico`: hoy existe como `RuntimeException` con línea, lexema del
  token ofensivo, token encontrado (con nombre y lexema) y token esperado.
  Falta llevarlo a una **clase de datos** análoga a `ErrorLexico` (que además
  guarde la línea fuente) para reportar errores de esta etapa sin mezclarla con
  `ErrorLexico`.
- Un mecanismo de reporte análogo a `ResultadoLexicoListener` (o reuso de
  alguna interfaz común) para que el Módulo Principal pueda recibir los
  errores sintácticos igual que hoy recibe los léxicos, sin acoplar
  `AnalizadorSintactico` a `System.out` directamente (mismo principio que
  REQ-MP-02: toda salida por `System.out`, pero centralizada en la vista).
  Todavía sin diseñar: hoy `AnalizadorSintacticoImpl` lanza `ErrorSintactico`
  y `ModuloPrincipalET2` lo captura y lo imprime (un solo error por corrida).
- Un método por cada no terminal de la sección "Gramática LL(1) resultante" —
  **ya escrito** en `AnalizadorSintacticoImpl` (uno por no terminal, con el
  nombre pelado del no terminal en minúscula; ver "Estado actual del código"),
  más los de la lambda, la variable local clásica, la visibilidad, el `for`,
  los genéricos anidados/diamante, los inicializadores de atributo, los
  inicializadores de arreglo, el ternario (`ternarioOpcional()`) y el
  postfijo (`postfijoOpcional()`) de "Gramática Expandida (Logros)". **El
  Paso 5 está completo**: no queda ningún método pendiente de esta lista.

## Estado actual del código (pendientes detectados)

- `AnalizadorLexico.nextToken()` ya está implementado en `AnalizadorLexicoImpl`:
  modo *pull* sobre el mismo autómata que `startAnalizar()` (un token por
  llamada; saltea blancos y comentarios; los errores léxicos se siguen
  reportando por `listener.onError(...)` sin cortar la secuencia; devuelve `EOF`
  en la última llamada y en todas las siguientes). `src/Model` compila entero y
  `build.sh` pasa.
- `AnalizadorSintacticoImpl` ya es la traducción mecánica de la sección
  "Gramática LL(1) resultante": un método privado por cada no terminal, más la
  infraestructura `start()` / `match(TokenType)` / `avanzar()` / `actualEs()` /
  `actualEn()` / `error()` y los conjuntos FIRST como `EnumSet`. Compila de
  forma aislada (junto con `AnalizadorSintactico`, `ErrorSintactico`,
  `AnalizadorLexico`, `Token` y `TokenType`). `start()` y `match()` —que antes
  no compilaban— quedan resueltos.
  - Los métodos usan el nombre pelado del no terminal (`clase()`,
    `sentenciaIf()`), no la forma `analizar<NoTerminal>()` que menciona
    "Estrategia de análisis"; los que chocan con palabras reservadas de Java
    van con prefijo `sentencia...` (`sentenciaIf`, `sentenciaWhile`,
    `sentenciaReturn`).
  - Los 20 no terminales con producción `ϵ` (`listaClases`,
    `genericidadOpcional`, `herenciaOpcional`, `extensionOpcional`,
    `listaMiembros`, `listaMetodosInterfaz`, `dimensionesOpcionales`,
    `tipoGenericoOpcional`, `listaArgsFormalesOpcional`,
    `listaArgsFormalesResto`, `listaSentencias`, `expresionOpcional`,
    `elseOpcional`, `restoAsignacion`, `expresionCompuestaResto`,
    `referenciaResto`, `argsActualesOpcional`, `dimensionesConTamanioOpc`,
    `listaExpsOpcional`, `restoListaExps`) llevan un `else` vacío explícito
    (`// ϵ — no hace nada`) como punto de extensión futuro; es un no-op, no
    cambia el análisis y los 8 tests siguen pasando. Los no terminales sin `ϵ`
    mantienen el `else` que llama a `error(...)` (ver "Producciones anulables"
    en "Estrategia de análisis").
  - **Lambda (`REQ-AS-005`) ya implementada** según "Gramática Expandida
    (Logros)": se sumó el token `ARROW` (`->`) y `operando()` quedó reescrito
    con la factorización `<TrasId>` / `<TrasParen>` / `<TrasParenId>` /
    `<DecidirTrasCierre>` / `<ListaIdLambda>` / `<RestoListaIdLambda>`. Eso
    **inlinea y elimina** los métodos `referencia()`, `primario()` y
    `expresionParentizada()` (y con ellos la constante `PRIMEROS_PRIMARIO`),
    porque el reparto de los prefijos `(` e `idMetVar` entre lambda y
    referencia se hace dentro de `operando()`. El parser sigue decidiendo sólo
    con FIRST + un token; `ARROW` no está en ningún FIRST. La bifurcación por
    `idMetVar` en `<TrasParenId>` **rechaza en el propio sintáctico**
    `( a + b ) -> e` y casos similares (no hace falta filtrarlo en semántica).
  - **Variable local clásica (`REQ-AS-006`) ya implementada**: `sentencia()`
    suma ramas para `<TipoPrimitivo>` / `idGen` / `idClase` (esta última con
    `sentIdClase()` factorizando declaración vs. llamada estática por el token
    que sigue al `idClase`), más `restoDeclLocal()` / `masIdsLocal()` /
    `initLocalOpc()`. `PRIMEROS_SENTENCIA` suma `boolean` / `char` / `int` /
    `idGen`. La rama `<Expresion> ;` se prueba después de la de `idClase`. La
    forma con `var` (`varLocal()`) queda intacta.
  - **Visibilidad de miembros (`REQ-AS-007`) ya implementada**: se sumó el
    token `PR_PRIVATE` (palabra clave `private`) a `TokenType` y
    `TablaPalabrasClave`, y `<Miembro>` quedó reescrito como
    `visibilidad()` + `cuerpoMiembro()`. Como `public` deja de marcar al
    constructor, su prefijo `idClase` se factoriza en profundidad en
    `trasIdClaseMiembro()` (misma técnica que `sentIdClase()`): `(` ⇒
    constructor; cualquier otra cosa ⇒ atributo/método de tipo clase.
    `PRIMEROS_MIEMBRO` suma `PR_PRIVATE`.
  - **`for` (`REQ-AS-009`) ya implementado**, en sus dos formas (clásica con
    `;` y for-each con `:`): se sumó el token `PR_FOR` (el `:` ya existía como
    `DOS_PUNTOS`) y `sentenciaFor()` llama a `clausulasFor()`, que factoriza el
    prefijo compartido `<Tipo> idMetVar` entre ambas formas y decide con un
    token más (`:` vs. `=`/`;`) en `trasIdForTipo()`. El choque de `idClase`
    (tipo vs. llamada estática como inicialización) se resuelve en
    `trasIdClaseFor()`, misma técnica que `sentIdClase()`/`trasIdClaseMiembro()`.
    Las tres secciones son opcionales (`for (;;)`); el cuerpo es `<Sentencia>`,
    no `<Bloque>`; el for-each no admite `var`. `PRIMEROS_SENTENCIA` suma
    `PR_FOR`.
  - **Genéricos anidados y notación diamante (`REQ-AS-010`) ya implementados**:
    `instanciadoOParametrico()` recursa en su rama `idClase` (llama de nuevo a
    `tipoGenericoOpcional()`), lo que habilita anidado (`Caja<Lista<Item>>`) en
    todo lo que ya usaba `<TipoGenericoOpcional>`, sin tocar esos no
    terminales. El diamante (`new Foo<>()`) es aparte: `restoNew()` pasa a
    llamar a `tipoGenericoOpcionalNew()` (nueva, admite interior vacío vía
    `diamanteOTipo()`) en vez de `tipoGenericoOpcional()`; el resto de
    contextos de tipo no cambia, así que `Foo<> x;` sigue siendo error
    sintáctico. El cierre `>>` no necesitó ningún cambio en el léxico: ya
    tokeniza como dos `OP_MAYOR` sueltos (`estadoMayor()` sólo combina con
    `=`), cada uno consumido por un nivel distinto de anidamiento.
  - **Inicializadores de atributo (`REQ-AS-011`) ya implementados**:
    `restoMiembro()` suma una tercera rama `<OperadorAsignacion>
    <ExpresionCompuesta> ;` (p. ej. `int x = 5;`), con FIRST `{ = }` disjunto
    de `{ ; }` y `{ ( }` — sin conflicto LL(1) que factorizar, la primera
    extensión de esta lista sin ese trabajo. Ya cubre atributos de arreglo con
    inicializador `new` (`int[] arr = new int[5];`). `static` sigue sin
    producir atributos (fuera de alcance de este requerimiento).
  - **Inicializadores de arreglo entre llaves (`REQ-AS-012`) ya implementados**,
    sólo al construir con `new`: `<DimensionesNew>` reemplaza a la vieja
    `<DimensionesConTamanio>` en `restoNew()`/`restoNewIdClase()`. Tras el
    primer `[`, `trasCorcheteNew()` decide con un token más: `]` inmediato ⇒
    todos los corchetes van vacíos (`masCorchetesVaciosNew()`) y sigue un
    `{ ... }` obligatorio (`inicializadorArreglo()`); una expresión ⇒ la forma
    de tamaño de siempre, reusando `dimensionesConTamanioOpc()` sin cambios.
    No se pueden mezclar tamaño e inicializador, igual que en Java.
    `<ValorArreglo>` admite anidar otro inicializador, así que
    `new int[][]{{1,2},{3,4}}` sale sin costo adicional. Sin tokens nuevos
    (`LLAVE_A`/`LLAVE_C` ya existían para `<Bloque>`).
  - **Operador ternario (`REQ-AS-013`) ya implementado**: `expresionCompuesta()`
    suma `ternarioOpcional()` al final (`? <ExpresionCompuesta> :
    <ExpresionCompuesta>` u ϵ), enganchado en `<ExpresionCompuesta>` y no en
    `<ExpresionBasica>` para quedar con menos precedencia que cualquier
    operador binario, y la asociatividad a derecha sale gratis porque el
    `<valorFalse>` (`<ExpresionCompuesta>`) ya prueba su propio
    `ternarioOpcional()` — ver "Precedencia del operador ternario". Apareció
    un bug real al integrarlo: `trasParenId()` reconstruye `<Expresion>` a
    mano para el conflicto lambda-vs-paréntesis y se había olvidado de sumar
    `ternarioOpcional()` ahí, así que `(c > d ? c : d)` fallaba; quedó
    corregido. Token nuevo: `TokenType.INTERROGACION` (`?`), símbolo simple
    sin combinaciones. Cubierto por `sintCorrecto17/18.java` (precedencia y
    anidado/asociatividad) y `sintError41/42.java` (falta `:` / falta
    `<valorFalse>`).
  - **Postfijo `++`/`--` (`REQ-AS-014`) ya implementado**: `expresionBasica()`
    suma `postfijoOpcional()` al final de sus dos ramas (después de
    `operando()`), enganchado en `<ExpresionBasica>` y no en
    `<ExpresionCompuesta>` para que aplique a cada término de la cadena
    binaria y no sólo al primero — ver "Postfijo `++`/`--`". Mismo bug real
    que el ternario, en el mismo lugar: `trasParenId()` reconstruye
    `<Operando>`/`<Expresion>` a mano y se había olvidado de sumar
    `postfijoOpcional()` tras `trasId()`, así que `(a++)` fallaba; quedó
    corregido. `<PostfijoOpcional>` es recursiva (admite `a++--` como caso
    positivo, igual que Java real — ver "Postfijo `++`/`--`" para el bug de
    razonamiento que hubo en el camino). Sin tokens nuevos:
    `OP_INCREMENTO`/`OP_DECREMENTO` ya existían en `TokenType` y ya los emitía
    el léxico, sin usar hasta ahora en el sintáctico. Cubierto por
    `sintCorrecto19/20.java` (postfijo en cadena binaria, paréntesis,
    referencia encadenada, ternario, `for`, y encadenado `a++--`) y
    `sintError43.java` (prefijo `++` fuera de alcance).
  - **El Paso 5 queda completo**: `REQ-AS-005..014` implementados. Sigue
    pendiente la recuperación en modo pánico (`REQ-AS-008`): hoy `error()`
    lanza `ErrorSintactico` y corta en el primer error.
- `ErrorSintactico` existe como `RuntimeException` con línea, lexema del token
  ofensivo, encontrado y esperado. Falta la versión "clase de datos" con línea
  fuente y el mecanismo de reporte tipo listener (ver "Piezas que va a
  necesitar el diseño").
- `ModuloPrincipalET2` (en `View`) es el punto de entrada de la etapa 2: abre
  el fuente, arma `AnalizadorLexicoImpl` + `AnalizadorSintacticoImpl` (léxico
  en modo *pull*) y corre `start()`. Si termina sin errores imprime
  `[SinErrores]`; si atrapa un `ErrorSintactico` imprime una línea legible más
  la etiqueta `[Error:<lexema>|<linea>]` (mismo formato que el error léxico de
  `ModuloPrincipal`). Es un espejo de `ModuloPrincipal` (solo léxico) y no toca
  la cadena de la etapa 1. El wiring se hace directo en la vista: el paso por
  `AnalizadorHandler` y un listener sintáctico quedan pendientes.
- Los testers `TesterSintacticoDeCasosSinErrores` / `TesterSintacticoDeCasosConErrores`
  (en `src/test/java`, recursos en `resources/sintactico/{sinErrores,conErrores}/`)
  corren contra `ModuloPrincipalET2`. 63 casos (20 sin error + 43 con error),
  `OK (63 tests)`. Cobertura propia por extensión:
  - Lambda: `sintCorrecto05..07` y `09` (las cinco formas, contextos variados,
    anidadas/currificación, y currying con parámetro entre paréntesis en cada
    nivel) y `sintError05..12` y `18..24` (cuerpo entre llaves / varias
    sentencias —tanto con lista de parámetros como con uno solo entre
    paréntesis—, parámetro con tipo, sin cuerpo, coma colgante, sin flecha, sin
    `)` de cierre, sólo la flecha, parámetro malformado en medio de una lista,
    y los casos que `<TrasParenId>` rechaza en el propio sintáctico en vez de
    diferir a semántica: `(a + b) -> e`, llamada a método, asignación, índice
    de arreglo y paréntesis anidados) — con 0, 1, pocos y muchos parámetros.
  - Variable local clásica: `sintCorrecto08` (`int x;`, `int x, y, z = 10;`,
    `T g;`, `Foo x;`, `Foo<Bar> x = new Foo<Bar>();`, conviviendo con `var` y
    con `Clase.metodo();`) y `sintError13..17` (sin `;`, nombre no idMetVar,
    coma colgante, dos nombres sin coma, init vacío).
  - Visibilidad de miembros: `sintCorrecto10` (`public`/`private`/implícita en
    atributos —incluido uno de tipo clase genérica—, métodos y constructores,
    conviviendo en la misma clase) y `sintError25..26` (orden invertido,
    `static private`; doble visibilidad, `public private`).
  - `for`: `sintCorrecto11` (clásica completa, las tres cláusulas vacías,
    combinaciones parciales, tipo primitivo/clase/genérico, for-each,
    inicialización con llamada estática, cuerpo con y sin llaves, anidado),
    `sintCorrecto12` (clásica completa: tipo primitivo, `idGen`, `idClase` con
    llamada al constructor, e inicialización por asignación a variable
    existente) y `sintCorrecto13` (for-each completo: tipo primitivo, `idGen`,
    `idClase` simple y genérico, cuerpo con y sin llaves, anidado) y
    `sintError27..30` (lista por coma en la inicialización, falta `;`
    separador, `,` en vez de `:` en for-each, `for` sin paréntesis) y
    `sintError31..33` (falta el `;` que delimita cada sección —init, cond,
    act—: el contenido de una sección puede omitirse, pero no su `;`; al
    faltar, la sección siguiente "corre" un lugar y el error recién aparece al
    llegar al `)` de cierre sin el último `;` pendiente).
  - Genéricos anidados y notación diamante: `sintCorrecto14` (anidado en
    atributo, anidado triple —fuerza el cierre `>>>`, tres `OP_MAYOR`
    consecutivos—, en variable local clásica y en for-each; diamante en `new`
    con y sin tipo anidado del lado izquierdo; `new` sin genérico —tipo
    crudo— y con argumento explícito no-diamante) y `sintError34..36`
    (diamante fuera de `new` en una declaración, diamante en el tipo de un
    for-each, `>` de cierre faltante en un anidado).
  - Inicializadores de atributo: `sintCorrecto15` (tipo primitivo, `idClase`
    con `new`, arreglo con inicializador `new`, atributo sin inicializar,
    conviviendo con visibilidad, constructor y método) y `sintError37..38`
    (falta el `;` tras el inicializador, `==` en vez de `=`).
  - Inicializadores de arreglo entre llaves: `sintCorrecto16` (tipo
    primitivo, `{}` vacío, multidimensional anidado, tipo clase, y la forma
    de tamaño de siempre sin inicializador) y `sintError39..40` (tamaño e
    inicializador mezclados, coma colgante en el inicializador).
  - Ternario: `sintCorrecto17` (precedencia frente a operadores binarios en la
    condición y en el `valorFalse`, y con la condición entre paréntesis) y
    `sintCorrecto18` (anidado y asociativo a derecha en el `valorFalse`, con y
    sin paréntesis, y anidado en el `valorTrue` con paréntesis explícito — el
    caso que destapó el bug de `trasParenId()`, ver "Precedencia del operador
    ternario") y `sintError41..42` (falta `:` entre `valorTrue` y `valorFalse`,
    falta el `valorFalse` tras `:`).
  - Postfijo `++`/`--`: `sintCorrecto19` (sobre el primer y el segundo término
    de una cadena binaria, entre paréntesis —sólo y junto a otro operando—,
    sobre una referencia encadenada `a.b.c++`, dentro de un ternario, y en el
    incremento de un `for` clásico), `sintCorrecto20` (encadenado `a++--` y
    `a----++`, caso positivo — ver "Postfijo `++`/`--`" sobre por qué no es
    error) y `sintError43` (prefijo `++a` fuera de alcance de `REQ-AS-014`).

  Los 4 testers juntos (léxico + sintáctico) dan `OK (101 tests)`.
- `SIntaxis.md` solo tiene la introducción y notación (BNF, terminal/no
  terminal), no la gramática en sí; la gramática de partida y su versión ya
  transformada a LL(1) viven por ahora en la sección "Gramática" de este
  documento (la de partida copiada de `Reglas de Sintaxis MiniJava 2026.pdf`).

## Próximos pasos

1. Hecho — la gramática de partida ya está transformada a LL(1) en la sección
   "Gramática LL(1) resultante" (eliminación de la ambigüedad
   `<Asignacion>`/`<Llamada>`, de la recursión izquierda de
   `<ListaArgsFormales>` / `<ExpresionCompuesta>` / `<Referencia>`, y
   factorización de `<Miembro>` / `<Sentencia>` / `<Primario>` / `new`). Del
   **Paso 5** ya están integradas (sección "Gramática Expandida (Logros)") la
   **lambda (`REQ-AS-005`)** —con "Factorización del prefijo `(`" y el token
   `ARROW`—, la **variable local clásica (`REQ-AS-006`)** —con "Factorización
   de `<Sentencia>`"—, la **visibilidad de miembros (`REQ-AS-007`)** —con
   "Factorización de `<Miembro>`" y el token `PR_PRIVATE`—, el **`for`
   (`REQ-AS-009`)** —con "Factorización de `<For>`" y el token `PR_FOR` (el
   `:` ya existía como `DOS_PUNTOS`)— y los **genéricos anidados y notación
   diamante (`REQ-AS-010`)** —`<InstanciadoOParametrico>` recursa y
   `<RestoNew>` suma `<TipoGenericoOpcionalNew>`/`<DiamanteOTipo>`; el `<` /
   `>` / `>>` que quedaba como punto abierto **no era un problema real**: el
   léxico ya tokeniza `>>` como dos `OP_MAYOR` sueltos—, los
   **inicializadores de atributo (`REQ-AS-011`)** —tercera rama de
   `<RestoMiembro>`, sin conflicto LL(1) que factorizar— y los
   **inicializadores de arreglo entre llaves (`REQ-AS-012`)** —
   `<DimensionesNew>` reemplaza a `<DimensionesConTamanio>`; tras el primer
   `[`, un `]` inmediato compromete a la forma con `<InicializadorArreglo>`
   obligatorio en vez de la forma de tamaño—, el **ternario (`REQ-AS-013`)**
   —colgado de `<ExpresionCompuesta>` como sufijo opcional, con menos
   precedencia que cualquier operador binario y asociatividad a derecha sin
   recursión explícita (ver "Precedencia del operador ternario")— y el
   **postfijo `++`/`--` (`REQ-AS-014`)** —colgado de `<ExpresionBasica>`, al
   revés que el ternario, porque es una propiedad por operando y no de la
   expresión completa (ver la sección "Postfijo" más arriba)—. **El Paso 5
   queda completo**: `REQ-AS-005..014` ya están en la gramática.
2. Hecho — `AnalizadorSintacticoImpl` es la traducción de esa gramática (un
   método por no terminal, sobre el esquema `start()` / `match()` descripto
   acá), **incluidas la lambda** (`operando()` reescrito + `trasId()` /
   `trasParen()` / `trasParenId()` / `decidirTrasCierre()` / `listaIdLambda()` /
   `restoListaIdLambda()`; `referencia()` / `primario()` / `expresionParentizada()`
   inlineados), **la variable local clásica** (ramas nuevas en `sentencia()` +
   `sentIdClase()` / `restoDeclLocal()` / `masIdsLocal()` / `initLocalOpc()`),
   **la visibilidad de miembros** (`miembro()` reescrito como `visibilidad()` +
   `cuerpoMiembro()`, con `trasIdClaseMiembro()` factorizando el prefijo
   `idClase` del constructor; token `PR_PRIVATE` sumado a `TokenType` y
   `TablaPalabrasClave`), **el `for`** (`sentenciaFor()` + `clausulasFor()` /
   `trasIdForTipo()` / `trasIdClaseFor()` / `condFor()` / `actFor()`; token
   `PR_FOR` sumado a `TokenType` y `TablaPalabrasClave`) y **los genéricos
   anidados y diamante** (`instanciadoOParametrico()` recursa en su rama
   `idClase`; `restoNew()` llama a `tipoGenericoOpcionalNew()` /
   `diamanteOTipo()`, nuevos, en vez de `tipoGenericoOpcional()`; sin tokens
   nuevos), **los inicializadores de atributo** (`restoMiembro()` suma la
   rama `<OperadorAsignacion> <ExpresionCompuesta> ;`; sin tokens nuevos) y
   **los inicializadores de arreglo** (`dimensionesNew()` + `trasCorcheteNew()`
   / `masCorchetesVaciosNew()` / `inicializadorArreglo()` /
   `listaValoresArregloOpcional()` / `listaValoresArreglo()` /
   `restoValoresArreglo()` / `valorArreglo()`, reemplazando a
   `dimensionesConTamanio()` en `restoNew()`/`restoNewIdClase()`; sin tokens
   nuevos), **el ternario** (`expresionCompuesta()` suma `ternarioOpcional()`;
   token `INTERROGACION` sumado a `TokenType` y reconocido en
   `AnalizadorLexicoImpl`; corregido además un olvido de `ternarioOpcional()`
   en `trasParenId()` que rechazaba `(c > d ? c : d)`) y **el postfijo**
   (`expresionBasica()` suma `postfijoOpcional()`; sin tokens nuevos —
   `OP_INCREMENTO`/`OP_DECREMENTO` ya existían sin usar en el sintáctico—;
   corregido el mismo tipo de olvido en `trasParenId()`, que rechazaba
   `(a++)`). **Los métodos del Paso 5 están completos.**
3. Hecho — `nextToken()` implementado en `AnalizadorLexicoImpl` en modo *pull*
   sobre el mismo autómata que `startAnalizar()`, sin tocar el camino de la
   etapa 1. `src/Model` compila entero.
4. Parcial — `ModuloPrincipalET2` ya corre el sintáctico y reporta el primer
   error como `[Error:<lexema>|<linea>]` (los testers sintácticos pasan). Falta:
   recuperación en modo pánico (`REQ-AS-008`, hoy `error()` corta en el primer
   error), la versión "clase de datos" de `ErrorSintactico` con línea fuente, y
   un mecanismo de reporte análogo a `ResultadoLexicoListener` que saque el
   wiring de la vista y lo pase por `AnalizadorHandler`.
