///[Error:{|7]
// Cuerpo de lambda entre llaves con varias sentencias: prohibido en MiniJava
// (REQ-AS-005, el cuerpo es UNA unica expresion, no admite "{ ... }").
class LambdaErr{
    static void metodo()
    {
        var f = (a, b) -> { a = 1; b = 2; };
    }
}
