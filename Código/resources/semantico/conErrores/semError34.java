///[ErrorSem:ERR_INTERFAZ_DUPLICADA|7]
// Mismo caso que semError33 pero del lado 'extends' de una interfaz --
// 'interface Malo extends Uno, Uno' tambien debe ser error.
interface Uno {
}

interface Malo extends Uno, Uno {
}
