///[Error:{|7]
// Cuerpo entre llaves con un solo parametro entre parentesis: sigue
// prohibido (mismo caso que sintError06, pero vía decidirTrasCierre()).
class LambdaErr{
    static void metodo()
    {
        var f = (a) -> { a = 1; };
    }
}
