# Analizador Semántico

## Propósito

El analizador semántico toma las estructuras generadas por el sintáctico y:

- Termina de recopilar la información del programa y las relaciones entre entidades.
- Controla que el programa sea semánticamente válido.

## Organización

Se agrupa según el nivel donde se realizan los chequeos:

- Chequeo de declaraciones (Esta etapa)
- Chequeo de sentencias

**Estado actual**: el chequeo de declaraciones ya corre de punta a punta
(`TablaSimbolos.consolidar()`, invocado desde `ModuloPrincipalET3`) y tiene
tests en `Código/resources/semantico/{sinErrores,conErrores}/` + los
testers `TesterSemanticoDeCasosSinErrores`/`ConErrores`
(`Código/src/test/java/`) — ver `propuesta_casos_test_semantico.md` secciones
3.5/3.6 para el detalle de qué cubre cada archivo. El chequeo de sentencias
(variables locales, `ERR_VARIABLE_LOCAL_DUPLICADA`, resolución de
identificadores en expresiones, etc.) **no está implementado ni tiene casos
de prueba todavía** — queda explícitamente para cuando se diseñe esa sección
más abajo; no bajar casos de prueba de sentencias a
`resources/semantico/{sinErrores,conErrores}/` antes de eso.

## Chequeo de declaraciones

Se focaliza solamente en las declaraciones. Es decir, en controlar que toda
declaración sea semánticamente válida y en recopilar la información que falte
de las entidades que se declararon.

En un lenguaje como MiniJava podemos dividir fácilmente el programa en
declaraciones y sentencias:

- Todo lo que está dentro de un bloque es una sentencia (incluyendo el
  bloque), es decir, las cosas que se "ejecutan" en el programa.
- Todo lo que está fuera de un bloque es una declaración:
  - Clases
  - Atributos
  - Métodos

### Actividades principales

1. Chequear que toda entidad fue correctamente declarada.
2. Consolidar la tabla de símbolos.

Estas actividades dependen de las restricciones de declaración del lenguaje.

### Chequear que toda declaración fue correctamente declarada

En general implica chequear que:

- No haya nombres repetidos en el mismo contexto (ej: dos clases con el mismo
  nombre). [Puede realizarse directamente mientras se construye la TS en el
  sintáctico.]
- Todo nombre usado en una declaración haya sido declarado en el contexto
  adecuado (ej: el tipo de un atributo o el retorno de un método).
- Cuestiones propias de cada... _(pendiente de completar)_

**TODO — Chequeo de corrección.** Por ahora queda pendiente de diseñar. La
idea es que quede desligado de la construcción de la TS (que es lo que cubre
el EDT de la sección "Acciones semánticas sobre la gramática original"): la
construcción arma los objetos y los registra; el chequeo de corrección corre
**después**, recorriendo la `TablaSimbolos` ya consolidada (no el árbol de
parseo) — reglas que necesitan todo resuelto de antemano, como herencia
circular, compatibilidad de firmas al overridear un método heredado, o que
los tipos referenciados efectivamente existan. Es lo que ya insinúa
`Chequeable.estaBienDeclarado()` / `TablaSimbolos.estaBienDeclarada()` en el
código, todavía sin implementación real. Queda para más adelante.

## Tabla de símbolos: por qué no usar Singleton

La tabla de símbolos necesita ser "global" en el sentido de que hay una única
instancia viva durante la compilación de un programa. Eso no requiere el
patrón Singleton, y de hecho conviene evitarlo:

- **Acopla todo el analizador a estado estático global.** Usar
  `TablaSimbolos.getInstance()` desde cualquier lado esconde la dependencia:
  ya no se ve en la firma de los métodos que una clase necesita la tabla, y
  cualquier parte del código puede leerla o modificarla sin pasar por una
  interfaz clara.
- **Rompe el testing.** Los tests unitarios de chequeo semántico correrían
  todos en la misma JVM compartiendo el mismo estado estático. Sin un
  `reset()` manual entre tests, una corrida contamina a la siguiente.
- **No soporta múltiples análisis en la misma ejecución.** Si en algún
  momento se necesita analizar más de un archivo/programa en la misma
  corrida (por ejemplo, un batch de tests), el singleton obliga a resetear
  el estado global entre corridas en vez de simplemente crear una tabla
  nueva por análisis.
- **El argumento de eficiencia del profesor** probablemente apunta a que la
  implementación clásica de libro de un Singleton "seguro" usa
  `synchronized` en `getInstance()`, agregando overhead de sincronización
  innecesario en un compilador que corre en un solo hilo.

### Alternativa: instancia única pasada por referencia (sin Singleton)

En vez de acceso estático global, se logra el mismo efecto (una sola tabla
viva por compilación) mediante inyección de dependencia explícita:

1. Se instancia **una** `TablaSimbolos` en el punto de entrada del análisis
   (análogo a cómo `ModuloPrincipalET2` en
   `Código/src/View/ModuloPrincipalET3.java` instancia una única vez el
   `AnalizadorSintacticoHandler` y lo usa para toda la corrida).
2. Esa instancia se pasa **por referencia** a quien la necesite: al
   visitor/chequeador semántico, como parámetro de constructor o de método
   (`chequear(TablaSimbolos tabla)`).
3. Al ser un objeto común (no estático), cada corrida de análisis puede
   crear la suya propia. Esto es ideal para tests unitarios, donde cada test
   instancia su propia tabla sin pisarse con otros.

Mismo efecto práctico que un Singleton (una sola instancia por compilación),
sin el acoplamiento oculto ni los problemas de testing.

## Creación de la Tabla de Simbolos en MiniJava

<Clase> ::= class idC<Herencia>{<Miembro>}
<Herencia>::= extends idC | e

Declaración de clase: ¿Qué debemos hacer?

Dos estrategias:

- Delegando la inserción de los elementos en profundidad. La idea es que en cada
producción inserto el elemento vinculado a esa producción inserto el elemento vinculado a esa producción en la 
estructura correspondiente de la TS. [Requiere llevar cuenta de la clase actual, metodo actual,etc.]
- Retornando el/los elementos a insertar La udea es que en cada producción retorno el elemento vinculado a esa producción.
Es responsabilidad de quien contiene ese elemento.

**Comparando las dos estrategias:**

- **Estrategia A (delegar la inserción en profundidad).** El parser mantiene
  un objeto "abierto" (ej. `claseActual`) como estado compartido — variable
  de instancia del parser, o algo Singleton-like — y cada producción hija lo
  escribe directamente a medida que va reconociendo piezas
  (`claseActual.agregarMetodo(...)` apenas termina de parsear un método).
  Nadie retorna nada "hacia arriba": el efecto ya ocurrió como side-effect
  mientras el parser bajaba.
  - Ventaja: parece más simple de escribir al principio (no hay que ir
    devolviendo objetos por toda la cadena de `return`s).
  - Desventaja: es el mismo problema que ya se descartó para `TablaSimbolos`
    en "por qué no usar Singleton" — acopla todo a estado mutable
    compartido, rompe el testing de cada método en aislamiento, y es fácil
    de romper si algún método se olvida de "cerrar"/restaurar el estado al
    volver de la recursión (ej: un miembro terminando agregado a la clase
    equivocada por un bug de orden de llamadas).
- **Estrategia B (retornar el elemento).** Cada `analizar<NoTerminal>()`
  devuelve el objeto que construyó, como variable local de quien lo llama.
  Nadie escribe directo en un `Clase`/`Metodo` que no le pertenece: el
  objeto se arma completo y recién se conecta con su contenedor cuando ese
  contenedor lo recibe como valor de retorno.

**Decisión: se usa la Estrategia B** para ir creando las instancias
(`Atributo`/`Metodo`/`Constructor`/`Clase`) y conectándolas entre sí. Ver el
ejemplo concreto, con código, en "Ejemplo concreto: cómo se asocian tipo +
nombre al construir el objeto" (dentro de la sección EDT).

El contexto que igual hace falta pasar hacia abajo para poder construir bien
(`entGen` para resolver `idGen`, la propia `tabla` para buscar `idClase`)
**se pasa por parámetro explícito** a los métodos que lo necesitan, nunca
como estado mutable compartido (variable de instancia del parser o
Singleton) — mismo argumento que en "Tabla de símbolos: por qué no usar
Singleton".

`claseActual` **no hace falta en absoluto durante la construcción**: lo
único para lo que se hubiera usado (chequear que el nombre del constructor
coincida con el de la clase) es un chequeo de corrección, y esa categoría de
chequeo quedó explícitamente diferida (ver "TODO — Chequeo de corrección"
más arriba). Alcanza con que `Constructor` guarde el token de su propio
nombre — la comparación contra el nombre de la `Clase` que lo contiene se
hace después, cuando corra ese chequeo diferido sobre la `TablaSimbolos` ya
armada, no durante el parseo.

## Tipo Primitivo VS Tipo Referencia.

¿Como representar los tipos?
Los tipos primitivos y referencia son diferentes semánticamente y por lo tanto deben 
modelarse por separado (Los tipos primitivos NO son clases).

Por otra parte es importante modelarlos de manera uniforme para diseñar adecuadamente las entidades que lo utilizan
(métodos y varibales). Esto lleva a la necesidad de implementar una clase (jerarquía) `Tipo`,
en vez de seguir guardando el tipo como el `Token` crudo que devuelve el léxico
(que es lo que hacen hoy `Atributo.java`/`Parametro.java`).

### Por qué un `Token` no alcanza

Un `Token` es la representación **sintáctica** (lexema + línea + `TokenType`,
útil para el mensaje de error), no la representación **semántica** (qué
significa ese texto una vez resuelto):

- **Genéricos.** `<TipoReferencia> ::= idClase <TipoGenericoOpcional>`. Con
  `Lista<Item> a; Lista<Otro> b;`, `a` y `b` arrancan con el mismo token
  `idClase` (`"Lista"`) pero son tipos incompatibles. El resultado de
  `<TipoGenericoOpcional>` no cabe en el token — hay que cargarlo aparte, y en
  cuanto se hace eso ya se está reconstruyendo un objeto `Tipo`, aunque no se
  lo llame así.
- **Arreglos.** `<DimensionesOpcionales> ::= [ ] <DimensionesOpcionales> | ϵ`.
  `int` e `int[][]` arrancan con el mismo token `PR_INT`; la cantidad de
  `[ ]` se acumula consumiendo tokens adicionales, no es un campo de `Token`.
  Guardar solo el primer token pierde si era arreglo y de qué dimensión.
- **Referencias sin resolver.** Para chequear herencia/compatibilidad hace
  falta la `Clase` ya resuelta (para poder recorrer `extends`), no el nombre
  como string. Guardando solo el token `idClase` habría que volver a buscar en
  la `TablaSimbolos` por lexema cada vez que se necesite comparar o subtipar,
  en vez de resolver una sola vez.
- **Comparación de tipos.** `Token` no tiene noción de "soy compatible con".
  Sin una clase `Tipo`, cada chequeo (asignación, retorno, parámetro de
  llamada) terminaría comparando `tipo.getLexema().equals(otro.getLexema())`
  a mano, duplicando la lógica de compatibilidad en cada lugar en vez de
  tenerla centralizada en un solo método (`Tipo.esCompatibleCon(Tipo)`).

### Por qué un primitivo no es una `Clase`

La gramática ya distingue tres categorías de tipo base, no dos:

```
<TipoBase> ::= <TipoPrimitivo> | <TipoReferencia> | idGen
<TipoPrimitivo> ::= boolean | char | int
<TipoReferencia> ::= idClase <TipoGenericoOpcional>
```

`Clase` (y la `TablaSimbolos`, un `HashMap<String, Clase>`) modela algo
puntual: una entidad **declarada por el programador**, con nombre, que puede
heredar y tiene atributos/métodos que hay que chequear. Ningún primitivo
cumple esto: `boolean`/`char`/`int` no los declara nadie (son palabras
reservadas del léxico, no `idClase`), no tienen atributos ni métodos, no
heredan y no pueden aparecer en `<HerenciaOpcional>`. Modelarlos como
instancias de `Clase` obligaría a forzar campos que nunca se usan
(`atributos`/`metodos` siempre vacíos, `estaBienDeclarada()` sin nada que
validar).

### Diseño: jerarquía `Tipo`

Una interfaz o clase abstracta `Tipo`, con una implementación por cada forma
en que puede resolverse `<TipoBase> <DimensionesOpcionales>`:

- **`TipoPrimitivo`** — conjunto cerrado y fijo (`boolean`, `char`, `int`).
  No busca nada en la `TablaSimbolos`; alcanza con un enum o tres constantes.
- **`TipoReferencia`** — envuelve la `Clase` (o interfaz) ya resuelta desde la
  `TablaSimbolos`, más el `Tipo` resuelto de `<TipoGenericoOpcional>` si lo
  hay. Acá sí hay que buscar `idClase` y dar error semántico si no fue
  declarada.
- **`TipoGenerico`** — para `idGen`: no es primitivo ni referencia, es una
  variable de tipo válida solo dentro del scope de la clase/método que la
  declaró (`<GenericidadOpcional>`). Se resuelve contra el entorno de
  genéricos vigente, no contra la `TablaSimbolos`.
- **`TipoArreglo`** — envuelve cualquiera de los tres anteriores más la
  cantidad de dimensiones acumuladas en `<DimensionesOpcionales>`.

El paso de `Token` (sintáctico) a `Tipo` (semántico) ocurre una sola vez, al
procesar `<Tipo>` durante el chequeo de declaraciones, y ese `Tipo` resuelto es
lo que se guarda en `Atributo`/`Metodo`/`Parametro` de ahí en más — no el
`Token` original (que puede seguir guardándose aparte solo si hace falta para
reportar la línea de un error).

### Reglas de compatibilidad que dependen de esto

La separación no es solo prolijidad: cambia las reglas de chequeo de tipos de
la fase de sentencias.

- **Primitivos**: compatibilidad por identidad simple (¿es el mismo
  primitivo?). Sin subtipado entre `int`/`char`/`boolean` (salvo que la
  cátedra indique una regla puntual de promoción numérica, a confirmar), y
  `null` **no** es asignable a un primitivo.
- **Referencias**: compatibilidad por **subtipado**, recorriendo
  `extends`/`implements` en `Clase`; `null` sí es asignable a cualquier tipo
  referencia.
- **Genéricos (`idGen`)**: dentro del scope donde está vigente se comporta
  como un tipo opaco (solo igual a sí mismo, salvo bounds), y hay que
  instanciarlo al resolver un uso concreto del tipo parametrizado.


## EStructura de la Tabla de Símbolos
En algunos lenguajes hay partes de la tabla que se terminan de 
consolidar en el chequeo de declaraciones
- Vinculando a entidades que dependen de otras entidades que se declaran mas adelante

En los lengujaes orientados a objetos cuando tenemos herencia no conocemos todos los métodos que tiene una clase hasta que no
procesamos a sus ancestros. En esots lenguajes una vez finalizado el analisis sintactico se consolidan las tabla de metodos de las clases.
(Voy a utilizar esta ultima).


### Acciones semánticas sobre la gramática original

Estrategia elegida (la segunda de las dos que se plantearon en "Creación de la
Tabla de Símbolos en MiniJava"): cada producción **retorna** el objeto que
construye (`.decl`, `.tipo`, etc.) y es quien la contiene el que decide dónde
insertarlo — no hay estado global tipo "clase actual" mutándose a mano.

Dos atributos heredados se pasan explícitamente entre corchetes `[...]`
cuando una producción los necesita:

- `tabla` — la `TablaSimbolos` de la corrida (inyectada, no Singleton — ver
  "Tabla de símbolos: por qué no usar Singleton"). Se usa para insertar
  clases/interfaces y para resolver `idClase` contra una `Clase` ya conocida.
- `entGen` — nombre del parámetro de tipo genérico vigente (viene de
  `<GenericidadOpcional>` de la clase/interfaz que contiene la producción;
  `ϵ` si esa clase/interfaz no declaró ninguno). Hace falta para decidir, en
  `<TipoBase>` / `<InstanciadoOParametrico>`, si un `idGen` suelto es el
  parámetro de tipo válido en ese scope o un error semántico (ver "Diseño:
  jerarquía `Tipo`").

**Por qué dos pasadas sobre `<ListaClases>`.** `<TipoReferencia>` necesita
buscar `idClase` en `tabla`, pero MiniJava permite referencias hacia adelante
(una clase puede usar como tipo de atributo una clase declarada más abajo en
el archivo — ver "EStructura de la Tabla de Símbolos"). Con una sola pasada,
al procesar la primera clase del archivo la tabla todavía no tendría cargadas
las que aparecen después. Se resuelve con dos recorridos de `<ListaClases>`:
la Pasada 1 sólo registra nombres (para que existan todas las entradas), la
Pasada 2 resuelve cuerpos completos (ahí sí `tabla` ya está completa a nivel
de nombres).

#### Pasada 1 — registrar nombres

Sólo se evalúan estas acciones; el resto de cada producción (herencia,
miembros, tipos) no se procesa todavía en esta pasada.

```
<ListaClases> ::= <Clase> <ListaClases> | <Interfaz> <ListaClases> | ϵ

<Clase>    ::= class idClase ...
               { c := new Clase(idClase.lexema)
               ; tabla.insertarClase(c)          // error semántico si el nombre ya existe
               }

<Interfaz> ::= interface idClase ...
               { i := new Interfaz(idClase.lexema)
               ; tabla.insertarClase(i)          // mismo espacio de nombres que las clases
               }
```

#### Pasada 2 — resolver herencia, atributos y métodos

```
<Clase> ::= class idClase
              { c := tabla.buscarClase(idClase.lexema) }        // ya existe, viene de la Pasada 1
            <GenericidadOpcional>
              { c.paramTipo := GenericidadOpcional.nombre
              ; entGen := GenericidadOpcional.nombre }           // heredado al resto de la producción
            <HerenciaOpcional>[tabla, entGen]
              { c.padre     := HerenciaOpcional.tipoExtends
              ; c.interfaz  := HerenciaOpcional.tipoImplements }
            { <ListaMiembros>[tabla, entGen]
              { (atributos, metodos, constructores) := clasificar(ListaMiembros.miembros)
                // clasificar separa por tipo dinámico (Atributo/Metodo/Constructor) y
                // chequea nombres repetidos entre atributos y firmas repetidas entre
                // métodos — regla exacta de sobrecarga: pendiente, igual que en
                // "Chequear que toda declaración fue correctamente declarada"
              ; c.atributos     := atributos
              ; c.metodos       := metodos
              ; c.constructores := constructores
              }
            }

<GenericidadOpcional> ::= < idGen > { .nombre := idGen.lexema }
                        |  ϵ        { .nombre := ϵ }

<HerenciaOpcional> ::= extends <TipoReferencia>[tabla, entGen]
                          { .tipoExtends := TipoReferencia.tipo ; .tipoImplements := ϵ }
                      | implements <TipoReferencia>[tabla, entGen]
                          { .tipoExtends := ϵ ; .tipoImplements := TipoReferencia.tipo }
                      | ϵ
                          { .tipoExtends := ϵ ; .tipoImplements := ϵ }

<Interfaz> ::= interface idClase
                 { i := tabla.buscarClase(idClase.lexema) }
               <GenericidadOpcional>
                 { i.paramTipo := GenericidadOpcional.nombre
                 ; entGen := GenericidadOpcional.nombre }
               <ExtensionOpcional>[tabla, entGen]
                 { i.extiende := ExtensionOpcional.tipo }
               { <ListaMetodosInterfaz>[tabla, entGen]
                 { i.metodos := ListaMetodosInterfaz.metodos }
               }

<ExtensionOpcional> ::= extends <TipoReferencia>[tabla, entGen] { .tipo := TipoReferencia.tipo }
                       | ϵ                                      { .tipo := ϵ }

<ListaMiembros> ::= <Miembro>[tabla, entGen] <ListaMiembros>[tabla, entGen]
                       { .miembros := Miembro.decl :: ListaMiembros(2).miembros }   // cons
                   | ϵ
                       { .miembros := [] }

<Miembro> ::= <Atributo>[tabla, entGen]                    { .decl := Atributo.decl }
            | <Metodo>[tabla, entGen]                      { .decl := Metodo.decl }
            | <Constructor>[tabla, entGen]                 { .decl := Constructor.decl }

<Atributo> ::= <Tipo>[tabla, entGen] idMetVar ;
                 { .decl := new Atributo(idMetVar.lexema, Tipo.tipo, idMetVar.linea) }

<Metodo> ::= <ModificadorOpcional> <TipoMetodo>[tabla, entGen] idMetVar <ArgsFormales>[tabla, entGen] <Bloque>
               { .decl := new Metodo(idMetVar.lexema, TipoMetodo.tipo,
                                      ModificadorOpcional.esEstatico,
                                      ArgsFormales.parametros, idMetVar.linea)
                 // <Bloque> no se resuelve en esta etapa — ver "Chequeo de sentencias"
               }

<MetodoInterfaz> ::= <TipoMetodo>[tabla, entGen] idMetVar <ArgsFormales>[tabla, entGen] ;
                        { .decl := new Metodo(idMetVar.lexema, TipoMetodo.tipo,
                                               /* abstracto, sin cuerpo */ true,
                                               ArgsFormales.parametros, idMetVar.linea) }

<ListaMetodosInterfaz> ::= <MetodoInterfaz>[tabla, entGen] <ListaMetodosInterfaz>[tabla, entGen]
                              { .metodos := MetodoInterfaz.decl :: ListaMetodosInterfaz(2).metodos }
                          | ϵ
                              { .metodos := [] }

<Constructor> ::= public idClase <ArgsFormales>[tabla, entGen] <Bloque>
                     { .decl := new Constructor(idClase, ArgsFormales.parametros)
                       // idClase se guarda en el propio Constructor — la
                       // comparación contra el nombre de la clase contenedora
                       // queda diferida, ver "TODO — Chequeo de corrección"
                       // <Bloque> no se resuelve en esta etapa — ver "Chequeo de sentencias"
                     }

<ModificadorOpcional> ::= static { .esEstatico := true }
                         | ϵ     { .esEstatico := false }

<TipoMetodo> ::= <Tipo>[tabla, entGen] { .tipo := Tipo.tipo }
               | void                  { .tipo := TIPO_VOID }   // centinela, no es un Tipo de valor

<Tipo> ::= <TipoBase>[tabla, entGen] <DimensionesOpcionales>
             { .tipo := if DimensionesOpcionales.dim = 0
                        then TipoBase.tipo
                        else new TipoArreglo(TipoBase.tipo, DimensionesOpcionales.dim) }

<TipoBase> ::= <TipoPrimitivo>               { .tipo := TipoPrimitivo.tipo }
             | <TipoReferencia>[tabla, entGen] { .tipo := TipoReferencia.tipo }
             | idGen
                 { if idGen.lexema = entGen
                   then .tipo := new TipoGenerico(entGen)
                   else error("tipo genérico no declarado en este contexto: " + idGen.lexema, idGen.linea)
                 }

<DimensionesOpcionales> ::= [ ] <DimensionesOpcionales> { .dim := 1 + DimensionesOpcionales(2).dim }
                           | ϵ                           { .dim := 0 }

<TipoReferencia> ::= idClase <TipoGenericoOpcional>[tabla, entGen]
                        { clase := tabla.buscarClase(idClase.lexema)
                        ; if clase = null
                          then error("clase no declarada: " + idClase.lexema, idClase.linea)
                        ; .tipo := new TipoReferencia(clase, TipoGenericoOpcional.tipo)  // puede ser ϵ
                        }

<TipoPrimitivo> ::= boolean { .tipo := TipoPrimitivo.BOOLEAN }
                  | char    { .tipo := TipoPrimitivo.CHAR }
                  | int     { .tipo := TipoPrimitivo.INT }

<TipoGenericoOpcional> ::= < <InstanciadoOParametrico>[tabla, entGen] > { .tipo := InstanciadoOParametrico.tipo }
                          | ϵ                                           { .tipo := ϵ }

<InstanciadoOParametrico> ::= idGen
                                 { if idGen.lexema = entGen
                                   then .tipo := new TipoGenerico(entGen)
                                   else error("tipo genérico no declarado: " + idGen.lexema, idGen.linea)
                                 }
                             | idClase
                                 { clase := tabla.buscarClase(idClase.lexema)
                                 ; if clase = null
                                   then error("clase no declarada: " + idClase.lexema, idClase.linea)
                                 ; .tipo := new TipoReferencia(clase, ϵ)
                                 }

<ArgsFormales> ::= ( <ListaArgsFormalesOpcional>[tabla, entGen] )
                      { .parametros := ListaArgsFormalesOpcional.parametros }

<ListaArgsFormalesOpcional> ::= <ListaArgsFormales>[tabla, entGen] { .parametros := ListaArgsFormales.parametros }
                               | ϵ                                 { .parametros := [] }

<ListaArgsFormales> ::= <ArgFormal>[tabla, entGen]
                           { .parametros := [ArgFormal.decl] }
                       | <ListaArgsFormales>[tabla, entGen] , <ArgFormal>[tabla, entGen]
                           { .parametros := ListaArgsFormales(1).parametros ++ [ArgFormal.decl] }

<ArgFormal> ::= <Tipo>[tabla, entGen] idMetVar
                  { .decl := new Parametro(idMetVar.lexema, Tipo.tipo) }
```

**Después de las dos pasadas, sobre todas las clases ya resueltas** (no es
parte de la gramática, es un recorrido aparte sobre `tabla.clases`): se
consolida la tabla de métodos efectiva de cada clase (propios + heredados,
resolviendo redefiniciones), como ya se anotó en "EStructura de la Tabla de
Símbolos" ("una vez finalizado el análisis sintáctico se consolidan las
tablas de métodos de las clases").

### De la gramática original a la LL(1)

Cómo se traslada cada acción de arriba a la gramática factorizada que
realmente recorre el parser (`analizador_sintactico.md`, "Gramática Expandida
(Logros)"), según qué le pasó a cada no terminal:

- **No terminales solo factorizados** (la producción original quedó repartida
  en dos o más no terminales, pero sin agregarse recursión): la acción se
  mueve tal cual al punto donde en la LL(1) se termina de reconocer el mismo
  fragmento. Ej.: la acción de `<Atributo>` (crear el objeto `Atributo`) pasa a
  vivir en la rama correspondiente de `<RestoMiembro>`, que es donde la LL(1)
  recién sabe, por el lookahead, que no era un método.
- **No terminales a los que se les eliminó recursión a izquierda**
  (`<ListaArgsFormales>` es el caso de esta sección): el atributo sintetizado
  que antes se armaba "hacia arriba" por la recursión a izquierda pasa a ser un
  atributo **heredado con acumulador** que se arrastra a derecha por la cadena
  `<ListaArgsFormalesResto>`, y recién se sintetiza (se cierra la lista) al
  llegar al caso base `ϵ`.
- **No terminales nuevos sin contraparte en la gramática original**
  (`<CuerpoMiembro>`, `<TrasIdClaseMiembro>`, `<SentIdClase>`, etc.): no
  cargan significado propio, son solo el punto donde el parser consumió el
  token que decide la alternativa. La acción que en la gramática original era
  un `if` explícito (p. ej. "¿es `<Atributo>` o `<Metodo>`?" en `<Miembro>`)
  en la LL(1) sale gratis: ya viene decidido por **cuál** alternativa de
  `<CuerpoMiembro>` fue la que matcheó.
- **`tabla` y `entGen`** se siguen pasando igual, como parámetros de los
  métodos `analizar<NoTerminal>(...)` del parser — no cambia nada de esto al
  factorizar, porque factorizar no mueve en qué producción hace falta cada
  atributo heredado, solo en qué no terminal literal vive esa producción
  ahora.

### Ejemplo concreto: cómo se asocian tipo + nombre al construir el objeto

La sección anterior explica en general cómo trasladar cada acción a la
gramática LL(1). Acá va el mecanismo concreto para el caso que más cuesta
ver: `<CuerpoMiembro>` reconoce el tipo y el nombre de un miembro, pero
todavía no sabe si es un atributo o un método — esa decisión (y por lo tanto
la construcción del objeto) la termina tomando `<RestoMiembro>`, un no
terminal más abajo en la cadena de factorización.

Es la Estrategia B (ver "Creación de la Tabla de Simbolos en MiniJava")
funcionando en la práctica: como nadie puede escribir en un objeto ajeno ni
guardar "lo que venía viendo" en un campo compartido, cada método solo puede
**devolver** lo que ya construyó y **recibir como parámetro** lo que le
falta. El objeto final se arma recién en el método que junta la última pieza
que le faltaba.

**Caso 1 — `int total;` (atributo) vs `int suma(int a) {...}` (método).**
Los dos arrancan igual (`<TipoPrimitivo> <DimensionesOpcionales> idMetVar
...`); recién se distinguen mirando el token que sigue al nombre.

```java
private Chequeable cuerpoMiembro() {
    ...
    } else if (actualEn(PRIMEROS_TIPO_PRIMITIVO)) {
        Token tipoTok = tipoPrimitivo();      // (A) consigue el tipo
        dimensionesOpcionales();
        Token nombre = tokenActual;           // (B) consigue el nombre
        match(TokenType.ID_MET_VAR);
        return restoMiembro(tipoTok, nombre); // (C) empaqueta A+B y los pasa
    }
}

private Chequeable restoMiembro(Token tipoTok, Token nombreTok) {
    // tipoTok/nombreTok ya llegaron por parámetro — no hay que "ir a buscarlos"
    if (actualEs(TokenType.PUNTO_COMA)) {
        match(TokenType.PUNTO_COMA);
        return new Atributo(nombreTok, tipoTok);   // (D) recién acá se construye
    } else if (actualEs(TokenType.PAR_A)) {
        List<Parametro> params = argsFormales();
        bloque();
        return new Metodo(nombreTok, tipoTok, false, params);
    }
}
```

`cuerpoMiembro()` no construye nada — no puede, todavía no sabe qué es. Solo
junta `tipoTok`/`nombre` y se los entrega como argumentos a `restoMiembro()`,
que es quien tiene la última pieza (el token siguiente) y por eso es el único
lugar donde aparece `new Atributo(...)` / `new Metodo(...)`.

**Caso 2 — `Persona p;` (atributo de tipo clase) vs `Persona(int x) {...}`
(constructor).** Mismo mecanismo, un nivel más adentro, vía
`trasIdClaseMiembro(Token idClaseTok)`:

```java
private Chequeable cuerpoMiembro() {
    ...
    } else if (actualEs(TokenType.ID_CLASE)) {
        Token idClaseTok = tokenActual;
        match(TokenType.ID_CLASE);
        return trasIdClaseMiembro(idClaseTok);
    }
}

private Chequeable trasIdClaseMiembro(Token idClaseTok) {
    if (actualEs(TokenType.PAR_A)) {
        // "(" pegado al idClase → es CONSTRUCTOR; idClaseTok es el nombre
        List<Parametro> params = argsFormales();
        bloque();
        return new Constructor(idClaseTok, params);
    } else {
        // si no, idClaseTok era el TIPO de un atributo/método
        tipoGenericoOpcional();
        dimensionesOpcionales();
        Token nombre = tokenActual;
        match(TokenType.ID_MET_VAR);
        return restoMiembro(idClaseTok, nombre);   // MISMO restoMiembro() del Caso 1
    }
}
```

`restoMiembro()` es el mismo método en los dos casos: no le importa si el
tipo vino de `<TipoPrimitivo>` o de `idClase`, una vez que lo recibió como
parámetro ya no necesita saber de dónde salió.

**Regla general:** el objeto se construye en el método más profundo que sea
el primero en tener disponibles *todas* las piezas que ese constructor
necesita. Todo lo recolectado antes de ese punto viaja como parámetro de un
método a otro — nunca como campo del parser. Así se ve, en código, la
Estrategia B: si en cambio se hubiera usado la Estrategia A, `cuerpoMiembro()`
habría necesitado escribir `tipoTok`/`nombre` en algún lado compartido (un
campo del parser, o mutar un objeto a medio construir) para que
`restoMiembro()` los "encontrara" ahí — exactamente el acoplamiento que se
buscó evitar.

### Cuidado al pasar de `void` a un tipo de retorno: todo camino tiene que retornar

Java exige que un método que no es `void` **garantice, mirando solo el
código**, que cualquier camino de ejecución termine en un `return` o un
`throw`. No alcanza con que la gramática esté cubierta a mano (que las
ramas de un `if`/`else if` cubran todo el `FIRST` del no terminal) — el
compilador no razona sobre eso, solo ve el código literal. Si algún camino
se le escapa, tira `error: missing return statement`, y esto pega justo en
los métodos con cadenas largas de `if`/`else if` como `cuerpoMiembro()`.

Hay dos patrones, según si el valor a devolver depende de qué rama se tomó:

**1. El valor depende de la rama** (`cuerpoMiembro()`, `restoMiembro()`,
`trasIdClaseMiembro()`): cada rama necesita su propio `return`, **incluida
la rama de error/default**. "No tener nada que retornar" no existe en
Java — si conceptualmente no hay objeto, se retorna `null` explícitamente:

```java
private Chequeable cuerpoMiembro() {
    if (actualEs(TokenType.PR_STATIC)) {
        ...
        return new Metodo(...);
    } else if (actualEs(TokenType.PR_VOID)) {
        ...
        return new Metodo(...);
    } else if (actualEn(PRIMEROS_TIPO_PRIMITIVO)) {
        ...
        return restoMiembro(tipoTok, nombre);
    } else if (actualEs(TokenType.ID_GEN)) {
        ...
        return restoMiembro(tipoTok, nombre);
    } else if (actualEs(TokenType.ID_CLASE)) {
        ...
        return trasIdClaseMiembro(idClaseTok);
    } else {
        error("un miembro de clase (\"static\", \"void\" o un tipo)");
        return null;   // rama que en la práctica no debería alcanzarse
                        // (listaMiembros() ya filtra por PRIMEROS_MIEMBRO
                        // antes de llamar acá), pero el compilador no lo sabe.
    }
}
```

Ese `else` final con `return null;` es lo que deja conforme al compilador.
Y ese `null` no rompe nada río arriba: en `listaMiembros()` se agrega igual
a la lista, y en `clase()` el `instanceof Atributo`/`instanceof
Metodo`/`instanceof Constructor` da `false` para `null` en los tres casos —
se ignora solo. Es el comportamiento que conviene en modo pánico: un
miembro mal formado no aporta nada a la clase, pero tampoco corta la
construcción con una excepción.

**2. El valor NO depende de la rama** (`tipoPrimitivo()`, `tipoBase()`): si
el `if`/`else if` solo hace `match()` (efecto de lado) y lo que hay que
devolver ya se tenía antes de entrar al chequeo, alcanza con sacar el
`return` de adentro de cada rama y dejar **uno solo, al final, afuera del
`if`**:

```java
private Token tipoPrimitivo() {
    Token t = tokenActual;              // ya lo tengo, no depende de la rama
    if (actualEs(TokenType.PR_BOOLEAN)) match(TokenType.PR_BOOLEAN);
    else if (actualEs(TokenType.PR_CHAR)) match(TokenType.PR_CHAR);
    else if (actualEs(TokenType.PR_INT)) match(TokenType.PR_INT);
    else error("\"boolean\", \"char\" o \"int\"");
    return t;                            // un solo return, cubre todos los casos
}
```

Acá ni hace falta un `else` que retorne: como el `return t;` queda afuera
del `if`-chain, el compilador lo ve como código que se ejecuta siempre,
pase lo que pase adentro.

**Cómo elegir:** si el objeto a devolver es distinto en cada rama, patrón 1
(return por rama + `else` final con `return null;`). Si es el mismo dato
para todas las ramas y el `if`-chain solo decide cómo consumirlo, patrón 2
(un solo `return` al final).

## Chequeo de sentencias

### Por qué no se puede resolver como las declaraciones

`bloque()`/`listaSentencias()`/`sentencia()` (en `AnalizadorSintacticoImpl.java`)
hoy son puramente sintácticos: `void`, solo consumen tokens y validan la
gramática, no construyen nada. Por eso `Metodo.variablesLocales` está
declarado pero nunca se puebla.

A diferencia de clases/métodos/atributos, el chequeo de "¿esta variable usada
fue declarada?" **no admite dos pasadas**. Una clase puede usar como tipo de
atributo una clase declarada más abajo en el archivo (hay forward reference,
por eso Pasada 1 + Pasada 2). Una variable local **no**: en Java/MiniJava solo
es visible desde su declaración en adelante dentro de su bloque.

```java
int x = y; // error: y no existe todavía, no importa si se declara después
int y = 5;
```

Esto no es un detalle de implementación sino una regla del lenguaje: no hay
"hoisting" de locales. El chequeo tiene que ser **una sola pasada, en el
orden exacto del código fuente**, llevando el conjunto de nombres visibles
"hasta este punto" — no se puede consolidar de antemano como se hizo con
`tabla.clases`.

### Hace falta un AST de `Sentencia`/`Bloque`

Para poder recorrer el cuerpo de un método *después* de parsearlo (el
chequeo de corrección corre sobre estructuras ya armadas, no durante el
parseo — mismo criterio que el resto de "Chequeo de corrección" más arriba),
hay que dejar de descartar el bloque y construir un árbol:

```java
public interface Sentencia { }
public class Bloque implements Sentencia { List<Sentencia> sentencias; }
public class DeclaracionLocal implements Sentencia { Token nombre; Tipo tipo; Expresion inicializador; }
public class SentenciaExpresion implements Sentencia { Expresion expr; }
public class SentenciaIf implements Sentencia { Expresion cond; Sentencia entonces; Sentencia sino; /* null si no hay else */ }
public class SentenciaWhile implements Sentencia { Expresion cond; Sentencia cuerpo; }
public class SentenciaReturn implements Sentencia { Expresion valor; /* null si es void */ }
```

Y `Metodo`/`Constructor` guardan ese `Bloque` (`private Bloque cuerpo;`) en vez
de que `bloque()` lo tire.

### Estrategia de construcción: Estrategia B anidada, con un único punto de attach

El código actual (`crearClase`/`crearMetodo` en `AnalizadorSintacticoImpl.java`,
vía `tablaSimbolo.setClaseActual`/`setMetodoActual`) ya usa **Estrategia A**
para dejar la `Clase`/`Metodo` "abierta" como placeholder — contradice la
decisión original de este documento, pero es un estado de transición
razonable: Pasada 1 crea el objeto vacío para poder referenciarlo, y se va
parchando (`tipoRetorno`, `params`) a medida que Pasada 2 lo resuelve.

Para el cuerpo del método **no** sirve extender esa misma idea a cada
sentencia (es decir, no hacer `metodoActual.agregarSentencia(nodo)` como
side-effect desde adentro de `sentencia()`). En cuanto hay anidamiento
(`if`/`while`/`for`/`{ }` anidado) se pierde la estructura:

```java
void foo() {
    if (cond) {
        int x = 5;   // si esto se "agrega" directo a metodoActual...
    }
    // ...queda al mismo nivel que el if en la lista del método, y se
    // pierde el dato de que x sólo era visible dentro del if.
}
```

La regla es la misma que para `cuerpoMiembro()`/`restoMiembro()`: **dentro de
la recursión de sentencias siempre se `return`**, y es el contenedor
inmediato (`SentenciaIf`, `SentenciaWhile`, el `Bloque` que envuelve) quien
recibe la pieza como parámetro de construcción — nunca un estado compartido.

```java
private List<Sentencia> listaSentencias() {
    if (actualEn(PRIMEROS_SENTENCIA)) {
        Sentencia s = sentencia();
        List<Sentencia> resto = listaSentencias();
        resto.add(0, s);
        return resto;
    }
    return new ArrayList<>();
}

private Bloque bloque() {
    match(TokenType.LLAVE_A);
    List<Sentencia> sentencias = listaSentencias();
    match(TokenType.LLAVE_C);
    return new Bloque(sentencias);
}

private Sentencia sentenciaIf() {
    match(TokenType.PR_IF);
    match(TokenType.PAR_A);
    Expresion cond = expresion();
    match(TokenType.PAR_C);
    Sentencia entonces = sentencia();
    Sentencia sino = elseOpcional();
    return new SentenciaIf(cond, entonces, sino);
}
```

`metodoActual` solo entra en juego **una vez**, en el borde exterior, cuando
`bloque()` ya devolvió el `Bloque` raíz completo (con todo el anidamiento
resuelto adentro) y se vuelve a `cuerpoMiembro()`/`restoMiembro()`/
`trasIdClaseMiembro()`:

```java
} else if (actualEs(TokenType.PAR_A)) {
    Metodo metodo = crearMetodo(nombreMiembro, false); // ya hace setMetodoActual()
    argsFormales();
    Bloque cuerpo = bloque();
    metodo.setCuerpo(cuerpo);   // único attach, al final — no sentencia por sentencia
```

### `Entorno`: scope para variables locales

Separado de `TablaSimbolos` (que resuelve clases/tipos globales), hace falta
una estructura más chica para resolver nombres locales — una pila/cadena de
scopes:

```java
public class Entorno {
    private Map<String, Tipo> visibles = new HashMap<>();
    private Entorno padre; // scope contenedor; null en la raíz del método

    public Tipo buscar(String nombre) {
        if (visibles.containsKey(nombre)) return visibles.get(nombre);
        return padre != null ? padre.buscar(nombre) : null; // null => no declarada
    }

    public boolean declarar(String nombre, Tipo tipo) {
        if (visibles.containsKey(nombre)) return false; // ya existe en ESTE scope
        visibles.put(nombre, tipo);
        return true;
    }
}
```

### Algoritmo: recorrido en orden sobre el `Bloque`

Corre **después** de que Pasada 2 consolidó `tabla` (clases, herencia,
atributos/métodos resueltos) — igual que el resto de "Chequeo de corrección":

1. Entorno inicial del método: atributos de `miClase` (propios + heredados —
   esos sí son visibles sin importar el orden textual, porque son
   declaraciones de clase, no locales) + los parámetros del método.
2. Recorrer `cuerpo.sentencias` **en orden**:
   - `DeclaracionLocal`: chequear el inicializador contra el entorno
     **actual** (sin la variable nueva todavía — así se detecta `int y = y;`);
     después `entorno.declarar(nombre, tipo)` — si devuelve `false`, error de
     nombre repetido en el mismo scope.
   - Sentencia con expresiones (asignación, llamada, condición de
     `if`/`while`/`for`, `return`): cada identificador usado se resuelve con
     `entorno.buscar(nombre)`; `null` ⇒ error semántico "variable no
     declarada".
   - `Bloque` anidado o cuerpo de `if`/`while`/`for`: crear un entorno hijo
     (`new Entorno(entornoActual)` como padre), recorrer con ese hijo, y
     descartarlo al salir — una variable declarada adentro no se filtra
     afuera del bloque que la contiene.

## Logros (Etapa 3)

1. **Sobrecarga — `sealed` y `final` (E3).** El compilador permite usar los
   modificadores `sealed` y `final` como en Java. `sealed` solo aplica a
   clases e interfaces, mientras que `final` aplica a clases, interfaces y
   métodos.

2. **Herencia múltiple (E3).** El compilador permite herencia múltiple de
   interfaces, como en Java.

3. **Métodos genéricos (E3).** El compilador permite declarar métodos
   genéricos, como en Java. Las reglas de redefinición para estos métodos
   siguen las mismas que en Java.

4. **Multi-detección de errores semánticos (E3).** El compilador no finaliza
   la ejecución ante el primer error semántico en una declaración, sino que
   se recupera y es capaz de reportar otros errores que tenga el programa
   fuente en la misma corrida. Como mínimo se espera que el compilador pueda
   continuar con el análisis de la siguiente declaración y que, cuando hay
   nombres repetidos, descarte ambas entidades.

5. **Genericidad avanzada.** El compilador controla que los tipos genéricos
   anidados sean utilizados correctamente en la declaración.

## Aclaraciones:
- Cuando el problema se da por un nombre repetido, el token es el que fue declarado
posteriormente desde el punto de visat sintáctico.
- Cuando un método esta mal redefinido el token corresponde con su nombre en la clse descendiente
  (el qu esta mal redefinido).
- Cuando hay un token refiriendo a un tipo no declarado, usar el lexema y linea de ese token y linea de ese token para 
reportar el error
- CUando no hay token claramente asociado a la clase del problema (por ejemplo un meotdo de interface n implmentado) usar el token del nombre de la entidad
que tiene el problema (en el ejemplo sería de la clase descendiente

## Convenciones y consideraciones
### Mensaje de analisis exitoso
CUando no se detectan errores en el analisis el modulo principal debería
mostrar un mensaje adecuado y luego el código de exito [Sin errores]

Class C1{
}

-> 

Compilación Exitosa 
[SinErrores]

## Diseño de la estructura de la Tabla de Simbolso
- Indentificar las entidades declarables
  - la idea es que cada entidad declrable serpa una clase
  - luego si una entidad puede contener otras entidades tendrablas/hasshs con esas entidades.
  
Chequeo de declaraciones
- COntrol de la correctitud de declaración de todos los elementos de la TS
- Consolidación de la TS

Estas tareas se pueden diseñar como funciones/metodos directamente sobre la TS y las clases de las entidades 

Para diseñar esta tareas la clave es valernos de la
Delegación y las Responsabilidades de cada entidad
● Por Ejemplo, para el control de correctitud
– La idea es que cada entidad controla lo que le corresponde (su
responsabilidad) y solicita a cada entidad que contiene que se
controle (delega el control)


Los controles de variables locales deben realizarse en el chequeo de sentencais
