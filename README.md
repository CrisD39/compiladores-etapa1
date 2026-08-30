# MiniJava — Analizador Léxico (Etapa 1)

Compiladores e Intérpretes — Departamento de Cs. e Ingeniería de la Computación, Universidad Nacional del Sur. Proyecto: implementación del analizador léxico de MiniJava.

Con esta entrega se busca cubrir la totalidad de los logros correspondientes a esta etapa del proyecto (análisis léxico): reconocimiento completo de los lexemas de MiniJava (palabras clave, identificadores, literales, símbolos de puntuación, operadores y fin de archivo), manejo de errores léxicos con recuperación (sin cortar la ejecución en el primer error), y una interfaz de línea de comandos que cumple el formato de salida pedido. El detalle de qué se cubre y cómo está documentado en el informe (ver más abajo).

## Cómo usar el programa

### Compilar

Desde `Código/`:

```bash
./build.sh
```

Esto compila el proyecto y arma `Código/out/interpretador.jar`.

### Ejecutar

```bash
java -jar Código/out/interpretador.jar <archivo-fuente>
```

`<archivo-fuente>` puede tener cualquier extensión. Por cada token reconocido se imprime una línea `(NombreToken,lexema,NroLinea)`; por cada error léxico se imprime el detalle con la línea fuente y un `^` apuntando al carácter que lo generó; al final, si no hubo errores, se imprime `[SinErrores]`.

Ejemplo:

```bash
$ java -jar Código/out/interpretador.jar programa1.java
(pr_class,class,1)
(idClase,Persona,1)
(llaveA,{,1)
...
(EOF,$,4)
[SinErrores]
```

### Correr los tests

```bash
cd Código
javac -cp "out/classes:lib/junit-4.13.2.jar:lib/hamcrest-core-1.3.jar" -d out/test-classes $(find src/test -name "*.java")
java -cp "out/classes:out/test-classes:lib/junit-4.13.2.jar:lib/hamcrest-core-1.3.jar" org.junit.runner.JUnitCore test.java.TesterDeCasosSinErrores test.java.TesterDeCasosConErrores
```

## Dónde está el resto de la documentación

Este README es solo una guía rápida de uso. **Todo el detalle técnico está en el informe**, en:

```
Documentación/Informe Analizador Léxico.md
```

Ahí se encuentra, entre otras cosas:

- La tabla completa de requisitos (REQ-MP-\*, REQ-AL-\*, REQ-AH-\*).
- La sección **Tokens**, con la expresión regular y las notas de cada token reconocido (palabras clave, identificadores, literales numéricos, char/string, puntuación, operadores, EOF).
- La arquitectura del programa (patrón en capas, interfaces, clases) y el diagrama del autómata finito (método por estado).
- La sección **Casos de prueba**, con los casos límite relevados durante el desarrollo (probados contra la implementación real, no derivados a mano), incluyendo aclaraciones sobre qué es alcance de esta etapa (léxica) y qué queda para la etapa sintáctica.

Documentos adicionales, también en `Documentación/`:

- `Guia_de_uso_testers.md`: cómo están armados y cómo agregar casos nuevos a los testers de JUnit (`resources/sinErrores/`, `resources/conErrores/`).
