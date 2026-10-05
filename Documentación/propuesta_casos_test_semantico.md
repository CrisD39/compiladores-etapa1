# Propuesta de Casos de Prueba — Analizador Semántico (Etapa 3)

> Estado: documento de propuesta, para discutir antes de implementar. Sigue el
> mismo criterio que `analizador_sintactico.md` ("este documento fija el
> contrato antes de terminar la implementación"): acá se decide **qué** se va
> a probar y **con qué oráculo**, antes de escribir `AnalizadorSemanticoImpl`.
> No modifica nada del código ni de `analizador_semantico.md`; donde hace
> falta una decisión que hoy no está tomada, queda marcada como pregunta
> abierta en vez de asumida.

## 0. Punto de partida: qué hay hoy y qué bloquea poder testear

El esqueleto de modelo ya existe (`Atributo`, `Chequeable`, `Constructor`,
`Interfaz`, `Metodo`, `Parametro`, `TablaSimbolos`, `Tipo`, `Clase`, todos en
`Código/src/Model/`), pero es un primer corte sin terminar. Antes de que
cualquier caso de los propuestos acá pueda correr, hace falta resolver (esto
es trabajo de implementación, no de diseño de tests — lo dejo anotado para que
se tenga en cuenta al planificar, no lo toco acá):

- `TablaSimbolos.estaBienDeclarada()` itera `clases` (un
  `HashMap<String, Clase>`) con `for (Clase c : clases)` — un `HashMap` no es
  `Iterable<Clase>`, no compila tal cual está.
- `Clase.estaBienDeclarado()` tiene el mismo problema con `metodos`
  (`HashMap<String, Metodo>`) y `atributos` (`HashMap<String, Atributo>`) —
  literalmente comentado `//No es iterable` en el código. Además itera
  `constructores` como `List<Constructor>`, pero el campo está declarado
  `List<Metodo>` (`agregarConstructor(Metodo constructor)` también recibe
  `Metodo`, no `Constructor`) — hay una inconsistencia de tipos entre
  `Constructor` (clase aparte) y cómo `Clase` guarda los constructores.
- `Atributo` y `Parametro` guardan el tipo como `Token` crudo
  (`private Token tipo`), no como `Tipo` — contradice directamente la sección
  "Por qué un `Token` no alcanza" del propio `analizador_semantico.md`
  (genéricos, arreglos y referencias sin resolver no entran en un `Token`).
- `Interfaz` no implementa `Chequeable`, no tiene getters, y
  `TablaSimbolos.insertarClase(Clase clase)` no acepta un `Interfaz` — pero el
  diseño documentado (Pasada 1 del EDT) dice `tabla.insertarClase(i)` con `i`
  una `Interfaz`, "mismo espacio de nombres que las clases".
- `TablaSimbolos` no tiene `buscarClase(String)`, que el EDT da por existente
  para resolver `idClase` en `<TipoReferencia>`/`<InstanciadoOParametrico>`.
- `Constructor.java` tiene un `import java.awt.*` sin usar y usa `List` sin
  importar `java.util.List`.

**Implicancia para este documento**: las secciones 3 y 5 están escritas contra
el *diseño* (el EDT de `analizador_semantico.md`), no contra el código actual.
Van a servir de contrato una vez que el modelo compile y tenga la forma que el
EDT ya supone (`Tipo` resuelto, `buscarClase`, `Interfaz` como `Chequeable`
insertable en la misma tabla).

## 1. Metodología de testing propuesta

Mismo criterio de "testing inteligente" que ya usan los testers de léxico y
sintáctico, adaptado a que acá hay más superficie (no es solo "aceptar o
rechazar una cadena", sino también *qué objetos quedaron en la tabla de
símbolos* después de un análisis válido):

- **Cobertura por regla del EDT**: cada acción semántica de
  "Acciones semánticas sobre la gramática original" (`analizador_semantico.md`)
  tiene al menos un caso positivo y, donde aplique un `error(...)`, al menos un
  caso negativo que lo dispare.
- **Partición de equivalencia sobre la jerarquía `Tipo`**: primitivo/primitivo,
  primitivo/referencia, referencia/referencia (con y sin subtipado),
  genérico/genérico, arreglo/arreglo (igual y distinta dimensión), `null`
  contra cada categoría — en vez de probar "todos los tipos contra todos",
  alcanza con un representante por combinación de categorías.
- **Casos límite**: 0 clases en el archivo, 0 miembros en una clase, cadena de
  herencia de longitud 1/2/N, genéricos anidados a 1/2/3 niveles, N clases con
  el mismo nombre (no solo 2).
- **Multi-error (Logro 4) como eje transversal**: todo caso negativo de la
  sección 3 debería tener, además de su versión aislada, al menos una variante
  combinada con otro error independiente en el mismo archivo, para confirmar
  que el análisis no corta en el primero — mismo rol que cumple
  `TesterSintacticoModoPanico` para el sintáctico.
- **Casos de regresión atados a decisiones de diseño ya escritas**, para que
  si alguien las cambia sin querer un test lo note: las dos pasadas sobre
  `<ListaClases>` (referencias hacia adelante), `null` no asignable a
  primitivo, el scope de `idGen` limitado a la clase/método que lo declaró.
- **Reuso del patrón de harness ya probado** en vez de inventar uno nuevo (ver
  sección 2): oráculo en comentario dentro del propio archivo de caso,
  carpetas separadas por resultado esperado, tester parametrizado para el caso
  general y un tester aparte no parametrizado para lo que un harness
  parametrizado no puede expresar (igual rol que
  `TesterSintacticoModoPanico`).

## 2. Infraestructura de test propuesta (espejo del patrón sintáctico)

Carpetas propuestas, paralelas a `resources/sintactico/`:

```
resources/semantico/
├── sinErrores/     (análisis válido; además de "no hay error" hay que poder
│                    afirmar algo sobre la TablaSimbolos resultante)
├── conErrores/     (un error semántico esperado, igual mecánica que
│                    TesterSintacticoDeCasosConErrores)
└── multiError/     (Logro 4: varios errores independientes en una corrida,
                     igual rol que resources/sintactico/panico/)
```

**Decisión tomada — formato del oráculo de error: opción (a), código por
categoría.** El sintáctico puede identificar un error esperado con un único
`TokenType` (`[Error:lexema|línea]`) porque el error *es* "no matcheó este
token". Un error semántico no tiene un "token esperado" en ese sentido —
"clase no declarada" y "nombre duplicado" son categorías distintas de error
sobre el mismo lugar del archivo. Se adopta un código fijo por regla violada
(`ERR_HERENCIA_CICLICA`, `ERR_TIPO_NO_DECLARADO`, ...), en la primera línea del
caso, con la misma convención de tres barras que ya usa `resources/sintactico/`
(`///[...]`, sin espacio — ver `sintError44.java` y siguientes):

```
///[ErrorSem:<CODIGO>|<línea>]
```

Ejemplo real, ver `resources/semantico/conErrores/semError01.java`:
`///[ErrorSem:ERR_HERENCIA_CICLICA|5]`. La alternativa descartada era un
substring laxo del mensaje de error (igual mecánica que
`TesterSintacticoDeCasosConErrores`, pero sobre texto libre en vez de un
código) — se prefirió el código fijo porque no depende de la redacción exacta
del mensaje y permite un `enum`/conjunto de constantes compartido entre el
analizador y los tests.

Para **sinErrores**, además de `[SinErrores]`, conviene que el tester pueda
afirmar algo sobre la tabla resultante (cuántas clases quedaron, de qué tipo
quedó resuelto tal atributo) — a diferencia del léxico/sintáctico, que solo
afirman sobre la secuencia de tokens/errores impresa. Esto puede necesitar que
`ModuloPrincipalET3` exponga la `TablaSimbolos` final además de la lista de
errores (mismo tipo de decisión que ya se tomó para `getErrores()` en el
sintáctico).

## 3. Casos de prueba — Chequeo de declaraciones

Organizados por la regla del EDT que cada uno ejercita. Los fragmentos son
MiniJava mínimos, no archivos completos.

### 3.1 Registro de clases/interfaces (Pasada 1)

- **Positivo**: `class A{} class B{}` — dos nombres distintos, ambos quedan en
  `tabla`.
- **Negativo**: `class A{} class A{}` — nombre de clase duplicado. Caso
  concreto ya escrito: `conErrores/semError07.java`. Ver sección 3.5 para la
  convención de reporte (sobre la segunda declaración) y el resto de los
  casos de declaración repetida.
- **Negativo**: `class A{} interface A{}` — mismo espacio de nombres entre
  clase e interfaz (asumiendo que se resuelve el gap de la sección 0 que hoy
  se lo impide a nivel de tipos). Caso concreto: `conErrores/semError08.java`.
- **Límite**: archivo con cero clases (`eof` solo) — tabla vacía, análisis
  trivialmente válido (ningún REQ lo prohíbe).
- **Límite**: N clases (≥10) con nombres únicos — confirma que no hay un tope
  arbitrario de tamaño de tabla.

### 3.2 Resolución de `<TipoReferencia>` / `idClase` (Pasada 2)

- **Positivo — referencia hacia adelante**: `class A{ B b; } class B{}` — el
  atributo de `A` usa un tipo declarado más abajo en el archivo; válido
  gracias a las dos pasadas.
- **Positivo — tipo recursivo**: `class Nodo{ Nodo siguiente; }` — una clase
  que se referencia a sí misma.
- **Negativo**: `class A{ Foo f; }` sin que exista `Foo` en ningún lado —
  "clase no declarada", apuntando a la línea de `Foo`.
- **Negativo**: `class A extends Foo{}` con `Foo` no declarado.
- **Negativo**: `class A{ Caja<Foo> c; }` con `Caja` existente pero `Foo` (el
  argumento genérico) no — el error debe apuntar al tipo anidado, no solo
  confirmar que `Caja` existe.

### 3.3 Tipo genérico (`idGen`) y su scope

- **Positivo**: `class Caja<T>{ T contenido; T obtener(){ return contenido; } }`
  — `T` usado dentro de la clase que lo declaró (atributo y retorno).
- **Negativo**: `class Foo{ T x; }` sin `<T>` en la declaración de `Foo` —
  "tipo genérico no declarado en este contexto".
- **Negativo**: fuga de scope entre clases —
  `class Caja<T>{} class Otra{ T x; }` (la `T` de `Caja` no existe en `Otra`).
- **Límite**: dos clases distintas, cada una con su propio `<T>` homónimo
  (`class A<T>{} class B<T>{}`) — scopes independientes, no debe haber
  confusión entre el `T` de una y el de la otra.

### 3.4 Jerarquía `Tipo` — compatibilidad

(Aplica ya a inicializadores de atributo, `REQ-AS-011`, aun antes de que exista
chequeo de sentencias.)

- Primitivo con el mismo primitivo: `int x = 0;`-equivalente a nivel atributo
  (`int x = 5;`) — válido.
- Primitivo con otro primitivo: `boolean b = 1;`-equivalente — inválido (sin
  conversión implícita entre `int`/`char`/`boolean`; **pregunta abierta**: si
  la cátedra pide alguna promoción numérica puntual, falta confirmarla).
- Referencia con subtipo: `class A{} class B extends A{} class C{ A a = new B(); }`
  — válido por subtipado.
- Referencia con supertipo (sentido inverso): `A a; B b = a;`-equivalente a
  nivel atributo — inválido sin cast explícito.
- `null` a una referencia: `class A{} class C{ A a = null; }` — válido.
- `null` a un primitivo: `int x = null;`-equivalente — inválido.
- Arreglos con igual tipo base e igual dimensión: `int[] a = new int[]{1,2};`
  — válido.
- Arreglos con distinta dimensión: `int[] a = new int[][]{{1}};`-equivalente
  (si lo deja pasar el sintáctico) — inválido en semántico (`int[]` ≠
  `int[][]`).
- Genéricos instanciados con distinto argumento: `Lista<Item> a = new Lista<Otro>();`
  — inválido aunque compartan la misma clase base (`Lista`).

### 3.5 Declaraciones repetidas (clases, miembros, parámetros)

Todas las categorías de "nombre repetido" del lenguaje que son **declaración**
en el sentido de "Organización" de `analizador_semantico.md` (clase, atributo,
método, constructor, parámetro) — no solo dentro de una clase, la misma regla
general ("¿dos declaraciones compiten por el mismo nombre en el mismo scope?")
aplica en capas anidadas: archivo → clase → lista de parámetros. Casos ya
bajados a archivos concretos en
`Código/resources/semantico/{sinErrores,conErrores,multiError}/` e
implementados: `TablaSimbolos.insertarClase`, `Clase.agregarAtributo`/
`agregarMetodo`/`agregarConstructor`, `Metodo`/`Constructor.estaBienDeclarado`.

**Fuera de alcance de esta sección — variables locales.** "¿Dos variables
locales compiten por el mismo nombre en el mismo scope?" es la misma pregunta
general, pero una variable local se declara dentro de un `<Bloque>`, que es
**sentencia**, no declaración (ver "Organización" en `analizador_semantico.md`:
"Los controles de variables locales deben realizarse en el chequeo de
sentencias"). Por eso **no hay, ni va a haber en esta etapa**, casos de
prueba de `ERR_VARIABLE_LOCAL_DUPLICADA` — los 5 negativos y los 3 positivos
de control que se habían bajado a archivo (variable repetida en el mismo
bloque, en un bloque anidado, tapando un parámetro, en la variable de un
`for`, en `int x, x;`, más los positivos de bloques hermanos y shadowing de
atributo) se borraron de
`Código/resources/semantico/{conErrores,sinErrores}/` porque necesitan un
`Entorno`/pila de scopes que recorra el `Bloque` ya construido (ver "Chequeo
de sentencias" en `analizador_semantico.md`), que todavía no existe. Van a
volver a bajarse a archivo cuando se diseñe esa etapa, no antes.

**Convención de reporte, común a toda la sección**: el error se marca sobre la
línea de la **segunda** declaración (la que "ya existe" cuando se intenta
registrar) — a diferencia de la convención de herencia circular (3.6), acá no
hace falta recorrer ningún grafo: alcanza con insertar en un mapa por scope y
fallar en el segundo intento con el mismo nombre, así que no depende de que
`TablaSimbolos` sea `LinkedHashMap` (ese requisito es específico del chequeo
de ciclos).

**Qué SÍ es una declaración repetida (casos negativos):**

| Archivo | Caso | Código |
| --- | --- | --- |
| `conErrores/semError07.java` | dos clases con el mismo nombre | `ERR_CLASE_DUPLICADA` |
| `conErrores/semError08.java` | clase e interfaz con el mismo nombre (mismo namespace) | `ERR_CLASE_DUPLICADA` |
| `conErrores/semError09.java` | dos atributos con el mismo nombre en una clase | `ERR_ATRIBUTO_DUPLICADO` |
| `conErrores/semError10.java` | dos métodos con el mismo nombre **y la misma firma** | `ERR_METODO_DUPLICADO` |
| `conErrores/semError11.java` | dos constructores con la misma lista de parámetros | `ERR_CONSTRUCTOR_DUPLICADO` |
| `conErrores/semError12.java` | dos parámetros con el mismo nombre en `<ArgsFormales>` de un método | `ERR_PARAMETRO_DUPLICADO` |
| `conErrores/semError13.java` | mismo caso que 12, pero en `<ArgsFormales>` de un constructor | `ERR_PARAMETRO_DUPLICADO` |
| `multiError/semMultiErrorVariasDeclaracionesRepetidas.java` | clase + atributo + parámetro duplicados en un mismo archivo, categorías distintas (Logro 4) | `ERR_CLASE_DUPLICADA`, `ERR_ATRIBUTO_DUPLICADO` y `ERR_PARAMETRO_DUPLICADO`, uno por categoría |

Esta sección no tiene (por ahora) un positivo de control propio: los tres que
había (`semCorrecto04-06`) eran justamente los casos "no es error porque son
scopes distintos" para variables locales, y se borraron junto con sus
negativos (`semError14-18`) — ver el aviso de "Fuera de alcance" arriba. Un
positivo de control para, por ejemplo, "mismo nombre de atributo en clases
distintas no es error" queda pendiente de bajar a archivo si hace falta.

**Preguntas abiertas que quedan** (no bajadas a archivo porque el oráculo
depende de una decisión que todavía no está tomada):

- **Regla exacta de sobrecarga** para métodos/constructores con el **mismo
  nombre pero distinta lista de parámetros** (`analizador_semantico.md`:
  "clasificar... regla exacta de sobrecarga: pendiente"). Los casos ya
  escritos (`semError10`/`semError11`) usan firma **idéntica**, que es
  duplicado bajo cualquier regla que se adopte — no hace falta esperar a esa
  decisión para esos dos. Los casos "mismo nombre, distinta firma" quedan
  pendientes hasta cerrar la regla.
- **Atributo y método con el mismo nombre** en la misma clase — ¿conviven en
  namespaces separados (como en Java) o es error acá? Sigue sin respuesta.
- **Parámetro de tipo genérico (`idGen` de `<GenericidadOpcional>`) vs. nombre
  de miembro** — p. ej. `class Caja<T>{ int T; }`: en Java conviven (son
  namespaces distintos, tipos vs. variables), pero no se confirmó si este
  proyecto sigue el mismo criterio. No bajado a archivo todavía.

### 3.6 Herencia (`extends`/`implements`) — chequeo de corrección básico

(Corre después de armar toda la `TablaSimbolos`, ver "TODO — Chequeo de
corrección" en `analizador_semantico.md`.)

**Casos ya bajados a archivos concretos** en
`Código/resources/semantico/{sinErrores,conErrores,multiError}/` (ver índice
abajo). Decisiones de diseño que fijan el oráculo de estos casos:

- **Un solo error por ciclo, no uno por clase involucrada.** Como
  `TablaSimbolos.estaBienDeclarada()` recorre todas las clases y llama
  `c.estaBienDeclarado()` en cada una, una implementación ingenua detectaría
  el mismo ciclo una vez por cada clase que lo integra (en `A→B→C→A`,
  reportaría 3 veces el "mismo" error). Se decidió deduplicar: el ciclo se
  detecta una única vez y las clases involucradas se marcan como inválidas sin
  volver a reportarlo — mismo espíritu que la deduplicación ya aplicada en el
  modo pánico del sintáctico.
- **Convención de qué clase "cierra" el ciclo (y por lo tanto en qué línea se
  reporta).** Recorriendo el grafo en **orden de declaración** (requiere que
  `TablaSimbolos.clases` sea un `LinkedHashMap`, no un `HashMap` — con
  `HashMap` el orden de iteración no está garantizado y la línea reportada
  dejaría de ser determinística, rompiendo el test), arrancando por la primera
  clase del ciclo que aparece en el archivo y siguiendo la cadena `extends`: el
  error se reporta sobre la **última clase visitada antes de volver a
  encontrar una ya vista en el recorrido actual** — la que, al seguir
  cerrando la cadena, apunta de nuevo hacia atrás. Es una nota de
  implementación, no solo de test: sin el `LinkedHashMap` la línea exacta no
  se puede afirmar de forma estable.
- **Distinguir "ciclo" de "tipo no declarado".** Si al caminar la cadena el
  padre de una clase no existe en la tabla (no es que ya se haya visitado, es
  que no está en ningún lado), no es un ciclo — es un `ERR_TIPO_NO_DECLARADO`
  normal (sección 3.2). Una implementación que no distinga los dos casos de
  corte ("ya visitado" vs. "no existe") puede reportar el error equivocado o,
  en el peor caso, colgarse si asume que siempre va a encontrar un padre.
- **La recursión por tipo de ATRIBUTO no es un ciclo de herencia.** Una clase
  con un atributo de su propio tipo (`class Nodo{ Nodo siguiente; }`) o dos
  clases que se referencian mutuamente por atributo (no por `extends`) son
  válidas — el grafo que hay que chequear por ciclos es el de
  `extends`/`implements`, no el de tipos de atributo. Casos de control
  dedicados a esto en `sinErrores/` para que una implementación que confunda
  ambos grafos falle un test enseguida.
- **El chequeo también aplica a `Interfaz.extendida`**, no solo a
  `Clase.herencia` — son dos campos/objetos distintos en el modelo actual, y
  conviene un caso dedicado (`semError06.java`) para no asumir que cubrir uno
  cubre el otro.

Índice de archivos (`sem` = semántico, numeración independiente de la del
sintáctico):

| Archivo | Caso | Oráculo |
| --- | --- | --- |
| `sinErrores/semCorrecto01.java` | cadena de herencia lineal sana (A←B←C) | `[SinErrores]` |
| `sinErrores/semCorrecto02.java` | autoreferencia por atributo, no es ciclo | `[SinErrores]` |
| `sinErrores/semCorrecto03.java` | composición mutua por atributo entre dos clases, no es ciclo | `[SinErrores]` |
| `conErrores/semError01.java` | ciclo directo (`A extends A`) | `ERR_HERENCIA_CICLICA\|5` |
| `conErrores/semError02.java` | ciclo indirecto, longitud 2 | `ERR_HERENCIA_CICLICA\|7` |
| `conErrores/semError03.java` | ciclo indirecto, longitud 3 | `ERR_HERENCIA_CICLICA\|7` |
| `conErrores/semError04.java` | tipo no declarado en `extends` (control, no es ciclo) | `ERR_TIPO_NO_DECLARADO\|6` |
| `conErrores/semError05.java` | cadena rota a mitad de camino por tipo no declarado (robustez) | `ERR_TIPO_NO_DECLARADO\|9` |
| `conErrores/semError06.java` | ciclo entre interfaces (`extends` de `Interfaz`) | `ERR_HERENCIA_CICLICA\|7` |
| `multiError/semMultiErrorDosCiclosIndependientes.java` | dos ciclos independientes en el mismo archivo (Logro 4) | dos `ERR_HERENCIA_CICLICA` (líneas 10 y 12) |
| `multiError/semMultiErrorCicloMasClaseSana.java` | un ciclo + una clase sana sin relación | un `ERR_HERENCIA_CICLICA` (línea 7), `Sana` no afectada |
| `multiError/semMultiErrorCicloLargoNoCuelga.java` | ciclo grande (50 clases), guardrail de robustez | un único error, con `@Test(timeout = ...)` |

Pendiente, **no bajado a archivo todavía**:

- **Negativo**: `class A implements Foo{}` con `Foo` una clase (no una
  interfaz) — mezcla de `implements` con algo que no es interfaz. Distinto de
  un ciclo; queda para cuando se diseñen los casos de chequeo de tipos en
  `implements`/`extends` en general.

## 4. Casos de prueba — Chequeo de sentencias (categorías propuestas, sin diseño previo)

`analizador_semantico.md` todavía no desarrolla esta parte (la nombra en
"Organización" pero el EDT solo cubre declaraciones). Antes de bajar esto a
casos concretos con oráculo exacto, conviene acordar las reglas — dejo acá
las categorías que parecen necesarias, a modo de lista de chequeo para esa
conversación, no como casos listos para implementar:

- **Asignación**: compatibilidad de tipos entre el lado izquierdo y el
  derecho (reusa la partición de 3.4).
- **Retorno**: el tipo de la expresión de `return` debe ser compatible con el
  tipo de retorno declarado; un método `void` no admite `return expr;`; un
  método no-`void` — **pregunta abierta**: ¿se exige que todo camino retorne,
  como en Java, o queda fuera de alcance de esta etapa?
- **Llamada a método/constructor**: cantidad y tipo de los argumentos
  actuales contra algún método/constructor declarado (depende de cómo se
  resuelva la sobrecarga, ver 3.5).
- **Acceso a variable**: todo `idMetVar` usado debe estar declarado en algún
  scope visible (parámetro, variable local, atributo propio o heredado).
- **Condición de `if`/`while`/`for`/ternario**: debe ser de tipo `boolean`.
- **Operadores binarios**: tipos de operandos compatibles con el operador
  (aritméticos solo entre `int`, `&&`/`||` solo entre `boolean`, `==`/`!=`
  entre tipos compatibles entre sí, comparaciones relacionales solo entre
  `int`, concatenación de `String` si existe como operador — a confirmar).
- **`this`**: válido solo dentro de un método/constructor de instancia, no en
  uno `static`.
- **Postfijo/prefijo (`++`/`--`)**: solo sobre algo asignable (variable,
  atributo, elemento de arreglo), no sobre un literal ni el resultado de una
  llamada — la gramática ya lo permite sintácticamente (`a++--` es válido
  sintáctico, ver "Postfijo" en `analizador_sintactico.md`); acá es donde se
  rechazaría semánticamente.

## 5. Casos de prueba por logro de Etapa 3

Para cada logro de `analizador_semantico.md` ("Logros (Etapa 3)"), indico si
hace falta extender la **gramática sintáctica** antes de que haya algo que
testear en semántico, y propongo categorías.

### 5.1 Logro 1 — `sealed` y `final`

**Gap sintáctico**: ni `sealed` ni `final` son palabras clave hoy (no están en
`TokenType` ni en `TablaPalabrasClave`, ni en `<Clase>`/`<Interfaz>`/
`<CuerpoMiembro>`). Hace falta una extensión de gramática antes (mismo tipo de
cambio que `PR_PRIVATE` para la visibilidad), con su propia factorización si
llega a compartir prefijo con algo existente.

Categorías propuestas una vez resuelto el gap:

- `sealed` en clase/interfaz: positivo; negativo si se usa en un
  atributo/método (`sealed` solo aplica a clase/interfaz, según el logro).
- `final` en clase: una clase que intenta extender una clase `final` →
  error.
- `final` en interfaz: **pregunta abierta** — ¿qué significa semánticamente
  ("no se puede extender" / "no se puede implementar")? No está definido.
- `final` en método: no puede redefinirse en una subclase.
- `sealed` + `final` juntos en la misma clase: **pregunta abierta** — ¿se
  permite la combinación o es contradictorio?

### 5.2 Logro 2 — herencia múltiple de interfaces

**Gap sintáctico crítico**: `<HerenciaOpcional> ::= extends <TipoReferencia> |
implements <TipoReferencia> | ϵ` admite un único tipo, y el caso existente
`sintError47.java` verifica explícitamente que `implements A, B` **es** un
error sintáctico hoy. Habilitar este logro implica:

1. Reescribir `<HerenciaOpcional>` (y probablemente `<ExtensionOpcional>`, si
   una interfaz también puede extender varias) para aceptar una lista de
   `<TipoReferencia>` separada por coma, con su propia recursión-a-derecha.
2. Decidir qué pasa con `sintError47.java` — ese caso deja de ser válido tal
   cual y hay que reescribirlo o reemplazarlo.
3. Esto es trabajo de **sintáctico** (una extensión de gramática más, en la
   línea de REQ-AS-005..014), no de semántico — propongo tratarlo como
   subtarea aparte antes de escribir los casos de este logro.

Categorías de test semántico, una vez resuelto el gap sintáctico:

- Clase que implementa 2+ interfaces sin métodos en conflicto — válida.
- Dos interfaces con un método de igual firma — se unifica sin error.
- Dos interfaces con un método de igual nombre y distinta firma — conflicto;
  **pregunta abierta** sobre si es error o cuál prevalece.
- Clase que implementa una interfaz sin definir todos sus métodos — error
  (método abstracto sin implementar).
- `implements A, A` (la misma interfaz repetida) — **pregunta abierta**: error
  o no-op.

### 5.3 Logro 3 — métodos genéricos

**Gap sintáctico**: `<GenericidadOpcional>` solo existe a nivel de
`<Clase>`/`<Interfaz>`; no hay forma de escribir `<T> T metodo(T x){...}` en
un método suelto. Hace falta extender `<CuerpoMiembro>` (o sumar una
alternativa) para que un método declare su propio parámetro de tipo, antes de
que haya algo que testear en semántico.

Categorías propuestas una vez resuelto el gap:

- Método genérico en una clase no genérica — el `T` del método es válido solo
  dentro de su propia firma/cuerpo.
- Método genérico cuyo parámetro de tipo tiene el mismo nombre que el de la
  clase contenedora — **pregunta abierta**: ¿shadowing como en Java real, o
  error?
- Override de un método genérico heredado — el logro dice "las reglas de
  redefinición siguen las mismas que Java"; como la gramática hoy no modela
  bounds, conviene confirmar qué parte de esa regla aplica sin bounds.

### 5.4 Logro 4 — multi-detección de errores semánticos

Este es el único logro que se puede testear **sin** extender la gramática —
conviene priorizarlo primero porque es transversal al resto. Ya tiene 4 casos
concretos de ejemplo (3 de herencia circular en la sección 3.6, uno de
declaraciones repetidas con tres categorías distintas en la 3.5 —
`multiError/semMultiErrorVariasDeclaracionesRepetidas.java`).

- Dos clases con nombre duplicado + una tercera clase con un tipo no
  declarado en el mismo archivo → ambos errores reportados en una sola
  corrida. **Pendiente de bajar a archivo** (variante no escrita todavía:
  combinar `ERR_CLASE_DUPLICADA` con `ERR_TIPO_NO_DECLARADO`, a diferencia de
  `semMultiErrorVariasDeclaracionesRepetidas.java`, que combina solo
  categorías de "repetido").
- "Descarta ambas entidades" (tal cual lo pide el logro): tras el error de
  nombre duplicado, verificar que **ninguna** de las dos clases quedó
  registrada — p. ej. una tercera clase que usa ese nombre como tipo debería
  fallar con "no declarada", porque ninguna de las dos sobrevivió. Todavía no
  bajado a archivo: requiere que `ModuloPrincipalET3` exponga la
  `TablaSimbolos` (pregunta abierta 2) para poder afirmarlo directamente, o
  diseñarlo indirectamente como se describe acá.
- **Límite**: N clases (no solo 2) con el mismo nombre — confirmar cuántos
  errores se esperan y que la tabla queda sin ninguna de las `N`. Pendiente.
- Mezcla de error sintáctico + error semántico en el mismo archivo — ambas
  listas deberían reportarse juntas (mismo patrón que ya usa
  `ModuloPrincipalET2` entre léxico y sintáctico). Pendiente.

### 5.5 Logro 5 — genericidad avanzada (anidados en la declaración)

El sintáctico ya acepta `Caja<Lista<Item>>` (`REQ-AS-010`); lo nuevo acá es el
chequeo **semántico** de que esos tipos anidados existan y se usen bien:

- Positivo: `Caja<Lista<Item>>` con `Lista` e `Item` declaradas.
- Negativo: `Caja<Lista<Foo>>` con `Foo` no declarada — el error debe
  apuntar al tipo anidado, no solo confirmar que `Caja`/`Lista` existen.
- Negativo: `idGen` anidado fuera de scope dentro de otro genérico
  (`Caja<T>` sin que la clase/método contenedor haya declarado `T`).
- Diamante (`new Foo<>()`): inferir el tipo del contexto
  (`Lista<Item> l = new Lista<>();` infiere `Item`) — esto ya pisa terreno de
  "chequeo de sentencias" (sección 4), queda marcada la dependencia.
- **Límite**: la clase/interfaz sigue limitada a un único parámetro de tipo
  (`<GenericidadOpcional>` no cambió) — confirmar que esto no bloquea anidar
  tipos con más de un nivel (`Caja<Lista<Mapa<K,V>>>` solo tiene sentido si
  `Mapa` en algún momento admite dos parámetros, lo cual hoy la gramática no
  permite; si se mantiene la restricción de un único parámetro, ese caso
  puntual queda fuera de alcance y conviene decirlo explícito en vez de
  dejarlo ambiguo).

## 6. Preguntas abiertas a resolver antes de escribir casos concretos

Recopiladas de las secciones de arriba, para discutir antes de implementar:

1. ~~Formato del oráculo de error semántico~~ — **resuelto** (sección 2):
   código por categoría, convención `///[ErrorSem:<CODIGO>|<línea>]`. Para
   herencia circular, **resuelto también** (sección 3.6): un solo error por
   ciclo, reportado sobre la última clase visitada en orden de declaración
   (requiere `TablaSimbolos.clases` como `LinkedHashMap`).
2. Si `ModuloPrincipalET3` necesita exponer la `TablaSimbolos` final (no solo
   la lista de errores) para que los testers de `sinErrores` puedan afirmar
   sobre el contenido resuelto.
3. Regla exacta de sobrecarga (mismo nombre vs. misma firma) para
   métodos/constructores duplicados **con distinta firma** (sección 3.5) — los
   casos de firma idéntica ya están resueltos y bajados a archivo
   (`semError10`/`semError11`), esto es solo para el caso "mismo nombre,
   distintos parámetros".
4. Si un atributo y un método pueden compartir nombre en la misma clase, y si
   un parámetro de tipo genérico (`idGen`) puede compartir nombre con un
   miembro (sección 3.5).
5. Si hay alguna promoción numérica implícita entre primitivos, o
   compatibilidad es por identidad estricta (sección 3.4).
6. Reglas de chequeo de sentencias en general (sección 4) — hoy no están
   definidas en `analizador_semantico.md`.
7. Semántica exacta de `final` en interfaz y de combinar `sealed` + `final`
   (sección 5.1).
8. Qué pasa con conflictos de firma entre interfaces distintas en herencia
   múltiple, y con `implements A, A` repetido (sección 5.2).
9. Shadowing entre el `T` de un método genérico y el `T` de su clase
   contenedora (sección 5.3).

## 7. Próximos pasos propuestos

1. **Hecho** — casos concretos de herencia circular (sección 3.6, 9 archivos:
   6 `conErrores` + 3 `sinErrores`, nombres de clase corregidos a 2+
   caracteres por REQ-AL-04/05 — una sola mayúscula lexa como `idGen`, no
   `idClase`) y de declaraciones repetidas **a nivel declaración** (sección
   3.5, 8 archivos: 7 negativos en `conErrores` +
   `semMultiErrorVariasDeclaracionesRepetidas.java`) están en
   `Código/resources/semantico/{sinErrores,conErrores,multiError}/`, con el
   formato de oráculo de la sección 2. Los casos de variable local
   (`ERR_VARIABLE_LOCAL_DUPLICADA`) que había en esta lista se sacaron — ver
   el aviso de "Fuera de alcance" en 3.5: son chequeo de sentencias, no de
   declaraciones, y vuelven cuando se diseñe esa etapa.
2. **Hecho** — resueltos los problemas de compilación del esqueleto (sección
   0) y `TablaSimbolos.clases` ya es `LinkedHashMap`.
3. **Hecho** — `TesterSemanticoDeCasosSinErrores`/`ConErrores`
   (`Código/src/test/java/`) ya corren contra
   `resources/semantico/{sinErrores,conErrores}/` sobre un pipeline semántico
   real (`ModuloPrincipalET3` → `AnalizadorSemanticoHandlerImpl` →
   `TablaSimbolos.consolidar()`), con las 13 reglas de declaración de la
   sección 3.5/3.6 pasando en verde. Pendiente: el tester no-parametrizado de
   `multiError` (análogo a `TesterSintacticoModoPanico`).
4. Cerrar las preguntas abiertas de la sección 6 que sean baratas de decidir
   ahora (2, 3, 4, 5) antes de escribir más casos concretos (tipos no
   declarados fuera de herencia, scope de `idGen`, sobrecarga con firma
   distinta).
5. Seguir bajando a archivos concretos el resto de la sección 3 (registro de
   clases ya cubierto por 3.5, resolución de tipos, scope de `idGen`,
   compatibilidad de `Tipo`), con el mismo patrón ya usado.
6. Implementar y testear en este orden, de menor a mayor dependencia de
   extensiones de gramática todavía no escritas:
   - **Declaraciones básicas** (sección 3, incluidas herencia circular y
     declaraciones repetidas) — no depende de ningún cambio sintáctico.
   - **Logro 4** (multi-error) — ya tiene 4 casos concretos de ejemplo
     (`multiError/`); extender el mismo patrón al resto de la sección 3.
   - **Logro 5** (genéricos anidados) — ya tiene soporte sintáctico completo.
   - **Logro 1, 2 y 3** (`sealed`/`final`, herencia múltiple de interfaces,
     métodos genéricos) — cada uno necesita primero una extensión de la
     gramática sintáctica (ver gaps en 5.1/5.2/5.3), a tratar como una
     revisión más de `analizador_sintactico.md` antes de volver acá.
7. Recién después de tener un primer borrador de "chequeo de sentencias" en
   `analizador_semantico.md` (hoy inexistente), bajar la sección 4 de este
   documento a casos concretos con oráculo.
