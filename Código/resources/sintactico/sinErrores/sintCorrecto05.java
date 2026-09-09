///[SinErrores]
// Prueba expresiones lambda (REQ-AS-005) en las cinco formas y una
// expresion parentizada normal, para verificar que el "(" no rompe.

class Prueba1{

    static void prueba1(int a)
    {
        var a1 = x -> x + 1;
        var b1 = () -> 0;
        var c1 = (p) -> p;
        var d1 = (p, q) -> p + q;
        var e1 = (1 + 2) * 3;
    }

}
