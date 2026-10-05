// Un ciclo (A<->B) conviviendo con una clase sana y sin relacion (Sana) en
// el mismo archivo: el ciclo no puede "contaminar" el resto del analisis.
// Harness esperado: containsString("[ErrorSem:ERR_HERENCIA_CICLICA|7]")
// (cierra en B) y, a la vez, Sana queda registrada y valida -- ninguna
// entrada de error deberia mencionar "Sana".
class A extends B{}
class B extends A{}
class Sana{}
