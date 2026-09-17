///[Error:+|7]
// Parametro malformado en el MEDIO de una lista de varios (no al final
// como sintError09): tras "a, b" se exige ")", no "+1".
class LambdaErr{
    static void metodo()
    {
        var f = (a, b+1, c) -> a;
    }
}
