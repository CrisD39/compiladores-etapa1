///[ErrorSem:ERR_HERENCIA_NO_PERMITIDA|13]
// Mala extiende dos interfaces: Base (sealed, que permite a Otra, no a
// Mala) y Comun (una interfaz normal) -- cada entrada de la lista de
// 'extends' se chequea de forma independiente contra su propio permits.
sealed interface Base permits Otra {
    void base();
}

interface Comun {
    void comun();
}

interface Mala extends Base, Comun {
    void extra();
}
