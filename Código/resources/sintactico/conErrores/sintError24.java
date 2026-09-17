///[Error:->|8]
// El idMetVar era el comienzo de un acceso a arreglo ("x[0]"), no un
// parametro: ejercita la rama "[" de <ReferenciaResto> reusada desde
// trasParenId().
class LambdaErr{
    static void metodo()
    {
        var f = (x[0]) -> x;
    }
}
