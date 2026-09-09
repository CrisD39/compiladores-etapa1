///[Error:a|7]
// Lista de parametros entre parentesis sin la flecha: tras "( ... )" con coma
// se exige "->" para que sea una lambda.
class LambdaErr{
    static void metodo()
    {
        var f = (a, b) a;
    }
}
