///[SinErrores]
// Currying con parametro entre parentesis en CADA nivel (a diferencia de
// sintCorrecto07, que usa mayormente parametros sin parentesis): ejercita
// decidirTrasCierre() de forma recursiva.

class LambdaCurryPar{

    static void metodo()
    {
        var curryPar  = (a) -> (b) -> (c) -> a + b + c;
        var mixNested = (x, y) -> (z) -> x + y + z;
    }

}
