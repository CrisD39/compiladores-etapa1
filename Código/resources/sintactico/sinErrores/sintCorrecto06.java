///[SinErrores]
// Lambdas validas (REQ-AS-005) en contextos variados y con distinta cantidad
// de parametros: ninguno, uno (con y sin parentesis), dos y muchos.

class LambdaOk{

    static Cosa metodo(int a)
    {
        var sinParam    = () -> 0;
        var unParam     = x -> x + 1;
        var unParamPar  = (y) -> y * 2;
        var dosParam    = (p, q) -> p + q;
        var muchosParam = (m, n, o, p2, q2, r) -> m + n + o + p2 + q2 + r;

        f(x -> x, () -> 1, (u, v) -> u);

        return z -> z;
    }

}
