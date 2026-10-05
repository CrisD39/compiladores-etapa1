///[Error:implements|5]
// <HerenciaOpcional> ::= "extends" <TipoReferencia> | "implements"
// <TipoReferencia> | ϵ -- es UNA de las dos ramas, no las dos encadenadas.
// Tras cerrar "extends Base" ya solo se espera "{".
class Malo extends Base implements Interfaz{

}

class Base{

}

interface Interfaz{

}
