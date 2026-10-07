///[ErrorSem:ERR_HERENCIA_NO_PERMITIDA|11]
// Clase sealed con permits de un solo nombre; otra clase, que no figura
// en esa lista, intenta extenderla de todos modos -- un solo nivel de
// chequeo, no importa que Circulo (el unico permitido) ni siquiera este
// declarado en este archivo.
sealed class Figura permits Circulo {

    int lados;
}

class Cuadrado extends Figura{

}
