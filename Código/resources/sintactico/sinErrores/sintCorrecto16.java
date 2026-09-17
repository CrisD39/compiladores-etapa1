///[SinErrores]
// Inicializador de arreglo entre llaves (REQ-AS-012), solo al construir con
// "new": tipo primitivo, vacio "{}", multidimensional anidado, tipo clase, y
// convive con la forma de tamaño de siempre (sin inicializador).

class ArregloOk{

    static void metodo()
    {
        var a = new int[]{1, 2, 3};
        var b = new int[]{};
        var c = new int[][]{{1, 2}, {3, 4}};
        var d = new Item[]{new Item(), new Item()};
        var e = new int[5];
        var f = new int[3][4];
    }

}
