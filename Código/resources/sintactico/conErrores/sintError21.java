///[Error:->|7]
// El idMetVar era el comienzo de una asignacion ("x = 1"), no un parametro:
// ejercita el reuso de restoAsignacion() dentro de trasParenId().
class LambdaErr{
    static void metodo()
    {
        var f = (x = 1) -> x;
    }
}
