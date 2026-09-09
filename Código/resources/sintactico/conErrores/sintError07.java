///[Error:int|7]
// Parametro de lambda con tipo explicito: prohibido en MiniJava
// (REQ-AS-005, los parametros son solo idMetVar, sin <Tipo>).
class LambdaErr{
    static void metodo()
    {
        var f = (int x) -> x;
    }
}
