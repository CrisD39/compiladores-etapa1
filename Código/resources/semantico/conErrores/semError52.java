///[ErrorSem:ERR_TIPO_NO_DECLARADO|5]
// El tipo de RETORNO de un metodo ahora tambien valida que su idClase exista
// (antes solo atributos y parametros lo hacian).
class Contenedor{
    Foo obtener(){
        return null;
    }
}
