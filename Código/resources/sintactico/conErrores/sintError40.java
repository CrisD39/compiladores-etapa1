///[Error:}|7]
// Coma colgante en el inicializador de arreglo: tras "," se espera otro
// valor, no el "}" de cierre.
class Foo{
    static void metodo()
    {
        var a = new int[]{1, 2,};
    }
}
