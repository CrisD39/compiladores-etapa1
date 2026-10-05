///[ErrorSem:ERR_HERENCIA_CICLICA|7]
// Mismo ciclo que semError02, pero entre INTERFACES (<ExtensionOpcional>)
// en vez de entre clases (<HerenciaOpcional>). El chequeo de ciclo tiene
// que recorrer tambien la cadena "extends" de Interfaz, no solo la de
// Clase -- son dos campos/objetos distintos en el modelo actual.
interface I1 extends I2{}
interface I2 extends I1{}
