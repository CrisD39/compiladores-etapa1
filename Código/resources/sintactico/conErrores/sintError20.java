///[Error:->|7]
// El idMetVar era el comienzo de una llamada a metodo (".metodo()"), no un
// parametro: el "->" final no tiene produccion que lo consuma.
class LambdaErr{
    static void metodo()
    {
        var f = (x.metodo()) -> x;
    }
}
