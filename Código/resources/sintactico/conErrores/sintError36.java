///[Error:x|7]
// Generico anidado con un ">" de cierre faltante: "Caja<Lista<Item>" solo
// cierra un nivel, falta el segundo ">" antes del nombre de la variable.
class Foo{
    static void metodo()
    {
        Caja<Lista<Item> x;
    }
}
