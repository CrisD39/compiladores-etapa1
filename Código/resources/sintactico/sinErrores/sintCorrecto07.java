///[SinErrores]
// Lambdas anidadas: el cuerpo de una lambda es a su vez una lambda
// (currificacion). Cada cuerpo sigue siendo UNA sola expresion.

class LambdaAnid{

    static void metodo()
    {
        var curry = a -> b -> c -> a + b + c;
        var mixto = (x, y) -> u -> x + y + u;
        var cero  = () -> () -> 0;
    }

}
