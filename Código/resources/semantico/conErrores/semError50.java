///[ErrorSem:ERR_TIPO_GENERICO_NO_DECLARADO|8]
// Logro 5: idGen anidado fuera de scope. "Contenedor" no es generica y no
// declara <T>, asi que el T dentro de Lista<T> no esta vigente.
class Lista<T>{
}

class Contenedor{
    Lista<T> elementos;
}
