///[SinErrores]
// Variables locales clásicas (REQ-AS-006): tipo explícito sin "var", una o
// varias por sentencia con un valor común al final. Convive con "var" y con
// las llamadas a método estático (Clase.metodo()).

class LocalOk{

    static void metodo(int a)
    {
        int x;
        int p, q, r = 10;
        boolean b = true;
        T g;
        Punto origen;
        Caja<Item> c = new Caja<Item>();

        var v = a + 1;
        Fabrica.crear();
        this.dato = g;
    }

}
