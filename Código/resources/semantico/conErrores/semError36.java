///[ErrorSem:ERR_METODO_RETORNO_INCOMPATIBLE|10]
// Alfa exige "int getValor()" y Beta exige "char getValor()" -- misma
// firma (nombre + parametros, sin tipo de retorno) pero retornos
// incompatibles -- conflicto real de Java, distinto de la sobrecarga.
interface Alfa {
    int getValor();
}

interface Beta {
    char getValor();
}

class Gama implements Alfa, Beta {
}
