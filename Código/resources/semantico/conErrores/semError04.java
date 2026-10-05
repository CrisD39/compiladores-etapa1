///[ErrorSem:ERR_TIPO_NO_DECLARADO|6]
// Caso de control: un "extends" hacia un tipo que directamente no existe.
// NO es un ciclo (no hay ningun camino que vuelva a "A") -- sirve para
// confirmar que el chequeo de ciclo y el de tipo-no-declarado son dos
// reglas distintas y no se pisan entre si.
class A1 extends Foo{}
