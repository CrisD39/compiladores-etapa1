///[ErrorSem:ERR_TIPO_NO_DECLARADO|9]
// Logro 5: el argumento generico "Foo" no esta declarado. El error tiene que
// apuntar al tipo anidado, no alcanza con que "Caja" exista.
class Caja<T>{
}

class Contenedor{

    Caja<Foo> contenido;
}
