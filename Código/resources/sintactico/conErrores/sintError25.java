///[Error:private|5]
// Orden invertido: la visibilidad va ANTES de "static" (y de cualquier otro
// prefijo de <CuerpoMiembro>), no al reves.
class Foo{
    static private void metodo()
    {
    }
}
