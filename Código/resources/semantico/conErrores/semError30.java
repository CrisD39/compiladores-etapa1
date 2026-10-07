///[ErrorSem:ERR_HERENCIA_NO_PERMITIDA|13]
// Pato implementa dos interfaces sealed: Volador (que SI lo permite) y
// Nadador (que permite a Ganso, no a Pato) -- cada entrada de la lista de
// 'implements' se chequea de forma independiente contra su propio permits.
sealed interface Volador permits Pato {
    void volar();
}

sealed interface Nadador permits Ganso {
    void nadar();
}

final class Pato implements Volador, Nadador {
    void volar(){
    }
    void nadar(){
    }
}
