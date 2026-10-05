///[Error:->|10]
// Parentesis anidados alrededor de una expresion COMPUESTA ("((x + y))",
// no un unico identificador como en sintError23): el parentesis interior ya
// se resuelve como expresion parentizada normal ("(x + y)"), y el exterior
// hereda esa expresion sin volver a ofrecer "->" tampoco.
class LambdaDobleParentesisCompuesta{
    static void metodo(){
        int x;
        int y;
        var f = ((x + y)) -> x;
    }
}
