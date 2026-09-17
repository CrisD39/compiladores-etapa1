///[Error:)|7]
// Falta la actualizacion (y su ";" que la marca vacia): tras la condicion no
// queda ningun ";" antes de ")".
class Foo{
    static void metodo()
    {
        for (int i = 0; i < 10)
        {
            var x = i;
        }
    }
}
