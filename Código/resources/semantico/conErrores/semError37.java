///[ErrorSem:ERR_METODO_RETORNO_INCOMPATIBLE|6]
// Gama declara su propio getValor() devolviendo char, pero Alfa exige
// que devuelva int -- no alcanza con tener la misma firma (nombre +
// parametros), el tipo de retorno tambien tiene que coincidir.
interface Alfa {
    int getValor();
}

class Gama implements Alfa {
    char getValor(){
        return 'x';
    }
}
