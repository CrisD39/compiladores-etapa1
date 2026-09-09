///[Error:)|7]
// Coma colgante en la lista de parametros de la lambda: tras "," se espera
// otro idMetVar, no ")".
class LambdaErr{
    static void metodo()
    {
        var f = (a, b,) -> a;
    }
}
