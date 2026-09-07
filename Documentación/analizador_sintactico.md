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
| REQ-AS-006 | El analizador sintáctico debe aceptar la declaración de variables locales en la forma clásica de Java: sin `var`, indicando el tipo (ejemplo `int x;`). Además debe admitir declarar varias variables e inicializarlas en una misma sentencia (ejemplo `int x,y,z = 10;`). Pendiente de reflejar en la gramática (ver nota en "Gramática"). |
| REQ-AS-007 | El analizador sintáctico debe permitir indicar la visibilidad de métodos, atributos y constructores de forma implícita o explícita: al declarar un miembro se puede omitir la visibilidad o indicar explícitamente `public` o `private`. Pendiente de reflejar en la gramática (ver nota en "Gramática"). |
| REQ-AS-008 | El compilador no finaliza la ejecución ante el primer error sino que se recupera (modo pánico) y es capaz de reportar otros errores. Para eso, al encontrar un error, el analizador descarta la entrada hasta encontrar un token de sincronización que permita reanudar el análisis: se espera que se sincronice con el siguiente punto y coma, llave de cierre, o llave que abre según el contexto (ver "Manejo de errores sintácticos"). |
| REQ-AS-009 | El analizador sintáctico debe aceptar sentencias `for` similares a las de Java, en dos formas: la estándar con separadores `;` y la de iteradores (for-each) con `:`. Se restringe cada sección del `for` a una sola sentencia/expresión (no listas separadas por coma). Pendiente de reflejar en la gramática (ver nota en "Gramática"). |
| REQ-AS-010 | El analizador sintáctico debe permitir tipos genéricos anidados y la notación diamante (`<>`), de forma similar a Java. Una clase o interfaz sigue limitada a un único parámetro de tipo. La notación diamante solo puede utilizarse al instanciar una clase genérica. Pendiente de reflejar en la gramática (ver nota en "Gramática"). |
| REQ-AS-011 | El analizador sintáctico debe permitir inicializar los atributos en el momento de su declaración, al igual que en Java. Pendiente de reflejar en la gramática (ver nota en "Gramática"). |
| REQ-AS-012 | El analizador sintáctico debe permitir la inicialización de arreglos al momento de su construcción, como en Java. Solo se admite como parte de una expresión de creación que utiliza `new`. Pendiente de reflejar en la gramática (ver nota en "Gramática"). |
| REQ-AS-013 | El analizador sintáctico debe permitir el operador condicional ternario dentro de las expresiones, como en Java (`condición ? expr_verdadera : expr_falsa`). Pendiente de reflejar en la gramática (ver nota en "Gramática"). |
| REQ-AS-014 | El analizador sintáctico debe permitir los operadores unarios postfijos de incremento (`++`) y decremento (`--`). Pendiente de reflejar en la gramática (ver nota en "Gramática"). |

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

**Detalle de REQ-AS-005 — forma sintáctica de la lambda.** Puntos a fijar,
todavía sin escribir la producción dentro del bloque de la sección "Gramática"
(se agrega en una revisión futura, igual que el resto de los ajustes):

- **Forma aceptada**: `<Lambda> ::= <ParamsLambda> -> <Expresion>`. El cuerpo
  reusa `<Expresion>` de la gramática (una sola expresión); no hay alternativa
  con `{ ... }`.
- **Parámetros sin tipo**: lista de `idMetVar` separados por `,`,
  opcionalmente entre paréntesis; con cero parámetros el `( )` es
  obligatorio (`() -> expr`). A diferencia de `<ArgFormal> ::= <Tipo> idMetVar`,
  acá nunca aparece `<Tipo>`.
- **Dónde enchufa**: la lambda es una forma de expresión; entra como
  alternativa a nivel de `<ExpresionBasica>` / `<Operando>` (a precisar al
  escribir la producción). Requiere sumar `->` como token nuevo del léxico si
  todavía no existe (`TokenType`), y `<Lambda>` + `<ParamsLambda>` como no
  terminales nuevos.
- **Conflicto LL(1) conocido**: el prefijo `(` es ambiguo entre
  `( <Expresion> )` (`<ExpresionParentizada>`), `( ) ->` y
  `( idMetVar , ... ) ->`. Con un solo token de lookahead no se distingue una
  lambda parentizada de una expresión parentizada. Es el punto más difícil de
  factorizar de esta extensión; la solución concreta (factorización profunda o
  un tratamiento especial del `(`) se define en la revisión de la sección
  "Gramática", no acá.
- **Restricción de captura / `this`**: es semántica (requiere resolución de
  nombres y alcances), fuera del alcance de esta etapa. Queda anotada en el
  requerimiento y se verifica en la etapa semántica posterior; el sintáctico
  solo valida la forma de la lambda.

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

Además falta **sumar la producción de expresión lambda** (`REQ-AS-005`):
`<Lambda> ::= <ParamsLambda> -> <Expresion>` como alternativa de expresión, con
`<ParamsLambda>` una lista de `idMetVar` sin tipo (opcionalmente entre `( )`,
obligatorio `( )` con cero parámetros). Al agregarla aparece un conflicto LL(1)
sobre el prefijo `(` — `( <Expresion> )` vs. `( ) ->` vs.
`( idMetVar , ... ) ->` — que hay que resolver por factorización profunda o un
tratamiento especial del `(`. Ver "Detalle de REQ-AS-005" en "Requerimientos".

Falta reflejar **variables locales clásicas** (`REQ-AS-006`): declaración sin
`var` con tipo explícito y con varias variables por sentencia — hoy `<VarLocal>`
solo cubre la forma `var idMetVar = ...`. A resolver en una revisión futura de
esta sección.

Falta reflejar la **visibilidad de miembros** (`REQ-AS-007`): `public` /
`private` opcional en atributos, métodos y constructores — hoy `<Atributo>` y
`<Metodo>` no tienen ranura de visibilidad, `<ModificadorOpcional>` solo cubre
`static`, y `<Constructor>` fuerza `public`. A resolver en una revisión futura
de esta sección.

Falta reflejar la sentencia **`for`** (`REQ-AS-009`): forma estándar con
separadores `;` y forma de iteradores (for-each) con `:`, con una sola
sentencia/expresión por sección — hoy `<Sentencia>` solo tiene `<If>` y
`<While>` como estructuras de control. A resolver en una revisión futura de esta
sección.

Falta reflejar los **genéricos anidados y la notación diamante** (`REQ-AS-010`):
hoy `<TipoGenericoOpcional> ::= < <InstanciadoOParametrico> >` solo admite un
`idGen`/`idClase` suelto, sin anidar, y no contempla `<>`. La clase/interfaz
sigue con un único parámetro de tipo (`<GenericidadOpcional>`) y el diamante solo
aplica al instanciar una clase genérica (`<LlamadaConstructor>`). A resolver en
una revisión futura de esta sección.

Falta reflejar la **inicialización de atributos en la declaración**
(`REQ-AS-011`): hoy `<Atributo> ::= <Tipo> idMetVar ;` no admite un
`= <Expresion>` antes del `;`. A resolver en una revisión futura de esta sección.

Falta reflejar la **inicialización de arreglos en la construcción**
(`REQ-AS-012`): hoy `<CrearArreglo> ::= new <TipoBase> <DimensionesConTamanio>`
solo admite dimensiones con tamaño y no contempla un inicializador entre llaves;
se admite únicamente como parte de una expresión de creación con `new`. A
resolver en una revisión futura de esta sección.

Falta reflejar el **operador condicional ternario** (`REQ-AS-013`):
`condición ? expr : expr` dentro de las expresiones — hoy la jerarquía de
`<Expresion>` / `<ExpresionCompuesta>` / `<ExpresionBasica>` no contempla
`? :`. A resolver en una revisión futura de esta sección.

Falta reflejar los **operadores unarios postfijos `++` y `--`** (`REQ-AS-014`):
hoy `<OperadorUnario> ::= + | - | !` es solo prefijo y no hay forma postfija
sobre un operando. A resolver en una revisión futura de esta sección.

Estos ajustes (eliminar recursión izquierda, reescribiendo esas producciones
como repetición a derecha con un no terminal auxiliar tipo `<...Resto>`;
factorizar donde haga falta) se van a ir aplicando en revisiones futuras de
esta misma sección, dejando registro de qué se cambió y por qué en vez de
reemplazar la gramática de origen en silencio.

### Reglas que violan LL(1) — análisis de la gramática de partida

Para un descenso recursivo predictivo con un token de lookahead, cada no
terminal con más de una alternativa tiene que poder elegir rama mirando un solo
token, y ningún no terminal puede ser recursivo a izquierda. Repasando la
gramática de partida tal como está en el bloque de arriba (sin contar todavía
las extensiones `REQ-AS-005..014`, que suman sus propios conflictos ya anotados
en las notas anteriores), estas son las reglas que lo impiden, agrupadas por
tipo de problema.

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

- **`<` / `>` como operador relacional y como delimitador de genéricos.** En la
  gramática de partida no chocan porque `<TipoGenericoOpcional>` /
  `<GenericidadOpcional>` solo aparecen en contexto de tipo y `<OperadorBinario>`
  solo en contexto de expresión. Con `REQ-AS-010` (genéricos anidados
  `List<List<X>>`, notación `<>`) el cierre `>>` y la decisión "¿`<` abre
  genérico o es menor-que?" pasan a necesitar tratamiento especial.
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
la gramática de partida, así que no hay regresión. Si más adelante se quiere
precedencia (p. ej. por `REQ-AS-013`), se introduce la escalera de no terminales
por nivel en ese momento.

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

#### Paso 5 — Al incorporar `REQ-AS-005..014`

Cada extensión pendiente vuelve a pasar por los pasos 1–3 sobre los no
terminales que toca. Dos ya tienen diagnóstico propio: el `(` de las lambdas
(`REQ-AS-005`, factorización profunda o mirada especial del `(`) y el `<` / `>`
de los genéricos anidados y `<>` (`REQ-AS-010`, incluye el cierre `>>`). El
resto (`for`, visibilidad, `var` clásico, inicializadores, ternario, `++`/`--`)
son alternativas nuevas que se factorizan con las mismas técnicas al agregarlas.

### Gramática LL(1) resultante

Gramática de partida con los pasos 0 a 2 ya aplicados (eliminación de la
ambigüedad `<Asignacion>`/`<Llamada>`, eliminación de recursión a izquierda y
factorización a izquierda). Misma notación BNF; no terminal inicial `<Inicial>`.

Alcance y límites de esta versión:

- **No** incorpora todavía las extensiones `REQ-AS-005..014` (lambdas,
  `for`, visibilidad de miembros, `var` clásico, genéricos anidados y diamante,
  inicializadores de atributo y de arreglo, ternario, `++`/`--`): son el
  **Paso 5**, y se van a ir sumando sobre esta base reaplicando los pasos 1–3 a
  los no terminales que toque cada una.
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

Nota de implementación pendiente: `AnalizadorLexicoImpl` hoy solo implementa
`startAnalizar()` (recorre el archivo completo empujando tokens al listener).
`nextToken()` todavía no tiene cuerpo — falta adaptar el autómata para que
pueda ejecutarse "un token por llamada" en lugar de "hasta EOF de una vez",
manteniendo el mismo reporte de errores léxicos.

## Gramática.


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

- `ErrorSintactico`: clase de datos análoga a `ErrorLexico` (línea, token
  encontrado, tokens esperados, línea fuente) para reportar errores de esta
  etapa sin mezclarla con `ErrorLexico`.
- Un mecanismo de reporte análogo a `ResultadoLexicoListener` (o reuso de
  alguna interfaz común) para que el Módulo Principal pueda recibir los
  errores sintácticos igual que hoy recibe los léxicos, sin acoplar
  `AnalizadorSintactico` a `System.out` directamente (mismo principio que
  REQ-MP-02: toda salida por `System.out`, pero centralizada en la vista).
- Un método `analizar<NoTerminal>()` por cada no terminal de la sección
  "Gramática" — no se pueden escribir en forma definitiva todavía porque esa
  gramática tiene recursión izquierda sin resolver (ver la nota al final de
  esa sección).

## Estado actual del código (pendientes detectados)

- `AnalizadorLexico.nextToken()` está declarado en la interfaz pero sin
  implementación en `AnalizadorLexicoImpl` (ver más arriba).
- `AnalizadorSintacticoImpl.match()` no compila: le falta tipo de retorno y
  cuerpo.
- `AnalizadorSintacticoImpl.start()` no compila: el `while` compara
  `tokenActual != TokenType.EOF`, pero `tokenActual` es un `Token` y
  `TokenType.EOF` es un `TokenType` — la comparación debería ser
  `tokenActual.getTipo() != TokenType.EOF`; además el cuerpo del `while` está
  vacío.
- `SIntaxis.md` solo tiene la introducción y notación (BNF, terminal/no
  terminal), no la gramática en sí; la gramática de partida vive por ahora en
  la sección "Gramática" de este documento (copiada de
  `Reglas de Sintaxis MiniJava 2026.pdf`), pendiente de los ajustes de LL(1)
  mencionados ahí.

## Próximos pasos

1. Ajustar la gramática de la sección "Gramática" a LL(1): eliminar la
   recursión izquierda señalada (`<ListaArgsFormales>`, `<ExpresionCompuesta>`,
   `<Referencia>`), confirmar que las producciones con `|` tengan FIRST
   disjuntos factorizando donde no sea así, y sumar la producción de expresión
   lambda (`<Lambda>` / `<ParamsLambda>`, `REQ-AS-005`) resolviendo el conflicto
   del prefijo `(` con `<ExpresionParentizada>`.
2. A partir de esa gramática, derivar el conjunto de métodos
   `analizar<NoTerminal>()` de `AnalizadorSintacticoImpl`, siguiendo el
   esqueleto de `start()`/`match()` descripto acá.
3. Implementar `nextToken()` en `AnalizadorLexicoImpl` sin romper
   `startAnalizar()` (los dos deberían poder convivir sobre el mismo
   autómata).
4. Definir `ErrorSintactico` y el mecanismo de reporte, y enchufar
   `AnalizadorSintactico` al `AnalizadorHandler` / Módulo Principal.
