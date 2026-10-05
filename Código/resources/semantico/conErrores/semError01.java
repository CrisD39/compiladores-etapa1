///[ErrorSem:ERR_HERENCIA_CICLICA|5]
// Ciclo directo, el caso mas chico posible: una clase que se extiende a si
// misma. El "ciclo" tiene un solo miembro, asi que es tambien el que cierra
// la cadena -- se reporta sobre su propia linea.
class A1 extends A1{}
