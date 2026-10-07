///[ErrorSem:ERR_TIPO_GENERICO_NO_DECLARADO|5]
// "T" usado como tipo de retorno sin estar declarado en ningun scope vigente
// (ni la clase ni el metodo declaran <T>).
class Foo{
    T m(){
        return null;
    }
}
