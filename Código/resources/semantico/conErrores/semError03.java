///[ErrorSem:ERR_HERENCIA_CICLICA|7]
// Ciclo indirecto de longitud 3 (A -> B -> C -> A). Misma convencion que
// semError02: se reporta sobre la ultima clase visitada en el recorrido
// (C, linea 7), que es la que vuelve a apuntar a una clase ya vista (A).
class A1 extends B1{}
class B1 extends C1{}
class C1 extends A1{}
