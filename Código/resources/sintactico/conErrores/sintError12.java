///[Error:->|7]
// Lambda sin parametros y sin parentesis: "->" no puede arrancar un operando,
// con cero parametros el "()" es obligatorio.
class LambdaErr{
    static void metodo()
    {
        var f = -> 0;
    }
}
