///[Error:->|7]
// "(a + b)" no es una lista de parametros valida (los parametros son
// idMetVar sueltos o una lista separada por comas): se parsea como expresion
// parentizada normal y el "->" que sigue queda sin produccion que lo consuma.
class LambdaErr{
    static void metodo(){
        var f = (a + b) -> a;
    }
}
