# Guía de uso de los testers léxicos

Esta guía explica únicamente cómo instalar, configurar, ejecutar y extender los testers entregados para el analizador léxico de MiniJava.

## 1. Instalación

Copiar los dos testers dentro de la carpeta de fuentes de test del proyecto:

```text
src/test/java/
├── TesterDeCasosSinErrores.java
└── TesterDeCasosConErrores.java
```

El proyecto debe tener disponibles las dependencias de test:

- JUnit 4
- Hamcrest

Los archivos de casos deben ubicarse en estas carpetas, relativas a la raíz del proyecto:

```text
resources/
├── sinErrores/
│   ├── lexSinErrores01.java
│   ├── lexSinErrores02.java
│   └── ...
└── conErrores/
    ├── lexConErrores01.java
    ├── lexConErrores02.java
    └── ...
```

Los testers utilizan exactamente estos paths:

```java
resources/sinErrores/
resources/conErrores/
```

Por ello, al ejecutar JUnit, el **working directory** debe ser la raíz del proyecto: el directorio que contiene la carpeta `resources`.

> No colocar archivos auxiliares en `resources/sinErrores/` ni en `resources/conErrores`. Cada archivo regular encontrado allí se ejecuta como un caso de prueba.

## 2. Configuración del main

Cada tester necesita conocer la clase que contiene el punto de entrada del compilador:

```java
public static void main(String[] args)
```

En ambos testers aparece una línea como esta:

```java
private static final Main init = null;
```

Reemplazar `Main` por la clase que contiene el método `main` de la implementación. Por ejemplo, si la clase principal se llama `ModuloPrincipal`:

```java
private static final ModuloPrincipal init = null;
```

Si la clase está en otro paquete, agregar o ajustar el `import` correspondiente.

El tester invoca el compilador pasando la ruta del caso como primer argumento:

```java
init.main(new String[] { pathDelCaso });
```

Por lo tanto, el `main` debe recibir la ruta del fuente mediante `args[0]`.

## 3. Ejecución

Ejecutar como tests JUnit las clases:

```text
TesterDeCasosSinErrores
TesterDeCasosConErrores
```

Ambas son pruebas parametrizadas. Cada archivo agregado a la carpeta correspondiente se ejecuta automáticamente como un caso independiente.

Para ver la salida capturada del compilador mientras se ejecutan los tests, cambiar temporalmente la bandera del tester correspondiente:

```java
private final boolean fullCompilerOutputPrintingInEachTest = true;
```

Luego se recomienda volver a dejarla en `false`.

## 4. Salida que usan los testers

Los testers invocan el compilador y capturan lo que se imprime por `System.out`.

Para un token, la salida debe tener esta forma:

```text
(NombreToken,Lexema,NroLinea)
```

Por ejemplo:

```text
(pr_class,class,1)
```

En los casos sin errores, el tester **ignora `NombreToken`**. Solo compara:

- el orden de los tokens
- el lexema
- el número de línea

Al finalizar correctamente, la salida debe contener:

```text
[SinErrores]
```

Ante un error léxico, la salida debe contener un código con esta forma:

```text
[Error:Lexema|NroLinea]
```

Ejemplo:

```text
[Error:#|2]
```

## 5. Crear casos sin errores

Crear un archivo dentro de:

```text
resources/sinErrores/
```

El caso contiene el fuente MiniJava y, al final, las expectativas de tokens. Cada expectativa se escribe como comentario simple:

```text
//#<lexema>,<numeroDeLinea>
```

El lexema es el texto exacto reconocido en el archivo fuente. Por eso los delimitadores de strings y caracteres forman parte del lexema esperado:

```java
"hola"
'c'
'\n'

//#"hola",1
//#'c',2
//#'\n',3
```

Las líneas `//#...` son comentarios simples para MiniJava, por lo que el compilador debe ignorarlas. El tester las lee como oráculos.

### EOF obligatorio

Todo caso sin errores debe incluir el token EOF. Se representa con el lexema especial `$`:

```text
//#$,<lineaEOF>
```

La expectativa de EOF debe ser la última línea de oráculo.

Ejemplo completo:

```java
class Alpha {
    var texto = "hola, mundo";
    var letra = '\n';

//#class,1
//#Alpha,1
//#{,1
//#var,2
//#texto,2
//#=,2
//#"hola, mundo",2
//#;,2
//#var,3
//#letra,3
//#=,3
//#'\n',3
//#;,3
//#},4
//#$,22
```

El número de línea de EOF corresponde al final físico del archivo. Como las anotaciones `//#...` son líneas del archivo, se deben tener en cuenta al calcular ese número. Por eso conviene escribir todas las expectativas al final y dejar `//#$,<lineaEOF>` como última línea.

### Lexemas con comas

El tester toma la **última coma** como separador entre lexema y línea. Se pueden usar lexemas que contengan comas:

```java
"hola, mundo"

//#"hola, mundo",1
//#$,4
```

### Qué verifica el tester

Para cada archivo de `resources/sinErrores/`, el tester verifica:

- Que se imprima `[SinErrores]`
- Que la cantidad de tokens sea igual a la esperada
- Que los tokens estén en el mismo orden
- Que cada lexema sea correcto
- Que cada número de línea sea correcto
- Que exista EOF, con lexema `$`, en la posición y línea esperadas

## 6. Crear casos con errores

Crear un archivo dentro de:

```text
resources/conErrores/
```

La primera línea debe contener el código de error esperado, usando exactamente este formato:

```java
// [Error:<lexema>|<numeroDeLinea>]
```

Ejemplo:

```java
// [Error:#|2]
hola # chau
```

El tester de errores obtiene la expectativa con:

```java
lineWithTheCode.substring(3)
```

Por eso la primera línea debe comenzar con dos barras seguidas de un espacio:

```text
// [Error:#|2]
```

En el ejemplo, la primera línea es un comentario y el carácter `#` aparece en la línea 2. El compilador debe imprimir una salida que contenga:

```text
[Error:#|2]
```

El tester no compara el texto completo del mensaje de error: comprueba que aparezca el código de error esperado.

## 7. Agregar casos nuevos

Para agregar un caso nuevo:

1. Elegir la carpeta según el resultado esperado:
   - `resources/sinErrores/` para un análisis exitoso
   - `resources/conErrores/` para un error léxico
2. Crear un archivo en esa carpeta. Puede tener cualquier extensión.
3. Si el caso es correcto, agregar una línea `//#<lexema>,<linea>` por cada token, incluido el EOF final `//#$,<lineaEOF>`.
4. Si el caso tiene error, colocar en la primera línea `// [Error:<lexema>|<linea>]`.
5. Ejecutar el tester correspondiente.

No es necesario registrar el nuevo caso en Java: ambos testers detectan automáticamente los archivos de sus carpetas y los ordenan por nombre.
