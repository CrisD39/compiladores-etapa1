///[ErrorSem:ERR_METODO_DUPLICADO|6]
// Dos metodos con la misma firma (nombre + tipos de parametros) dentro de
// la misma interfaz -- mismo chequeo que ya existia para Clase.agregarMetodo.
interface Uno {
    int m();
    int m();
}
