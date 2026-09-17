///[Error:->|8]
// Parentesis anidados alrededor de un solo identificador: la ambiguedad se
// resuelve primero adentro y despues afuera; en ningun nivel debe colarse
// el "->" final.
class LambdaErr{
    static void metodo()
    {
        var f = ((x)) -> x;
    }
}
