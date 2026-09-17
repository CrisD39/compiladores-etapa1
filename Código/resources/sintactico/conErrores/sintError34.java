///[Error:>|7]
// Notacion diamante fuera de "new": solo es valida al instanciar
// (REQ-AS-010), una declaracion de tipo sigue exigiendo un argumento real.
class Foo{
    static void metodo()
    {
        Caja<> x;
    }
}
