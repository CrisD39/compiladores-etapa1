///[SinErrores]
// Cadena de herencia lineal sana (A <- B <- C): no hay ciclo, cada clase
// extiende exactamente una clase ya declarada. Caso base de control antes de
// probar los casos de ciclo (semError01..03).
class A1{}
class B1 extends A1{}
class C1 extends B1{}
