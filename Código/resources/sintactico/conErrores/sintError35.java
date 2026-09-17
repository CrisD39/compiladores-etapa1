///[Error:>|7]
// Notacion diamante en el tipo de un for-each: tampoco es una instanciacion
// con "new", asi que sigue exigiendo un argumento real.
class Foo{
    static void metodo()
    {
        for (Caja<> c : cajas)
        {
        }
    }
}
