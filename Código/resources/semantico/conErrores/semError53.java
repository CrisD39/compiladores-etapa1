///[ErrorSem:ERR_TIPO_NO_DECLARADO|8]
// Logro 5 en un PARAMETRO: el argumento generico "Foo" no esta declarado.
// Mismo chequeo que en atributo/retorno, por el camino de Parametro.
class Caja<T>{
}

class Contenedor{
    void guardar(Caja<Foo> c){
    }
}
