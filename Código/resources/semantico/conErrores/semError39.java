///[ErrorSem:ERR_TIPO_GENERICO_NO_DECLARADO|9]
// Fuga de scope ENTRE CLASES: el T de Caja1<T> no esta vigente en Caja2 --
// cada Clase construye su propio entornoClase, no hay nada compartido entre
// entidades distintas de la tabla de simbolos.
class Caja1<T>{
}

class Caja2{
    T x;
}
