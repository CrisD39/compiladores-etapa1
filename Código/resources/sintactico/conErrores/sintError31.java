///[Error:)|8]
// Falta la inicializacion (y su ";" que la marca vacia): "i < 10" se toma
// como si fuera la inicializacion, "i = i + 1" como si fuera la condicion, y
// no queda ";" para cerrar antes de ")".
class Foo{
    static void metodo()
    {
        for (i < 10; i = i + 1)
        {
            var x = i;
        }
    }
}
