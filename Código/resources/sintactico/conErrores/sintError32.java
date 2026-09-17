///[Error:)|8]
// Falta el ";" que delimita la condicion (vacia o no): "i = i + 1" se toma
// como si fuera la condicion, y no queda ";" para la actualizacion antes
// de ")".
class Foo{
    static void metodo()
    {
        for (int i = 0; i = i + 1)
        {
            var x = i;
        }
    }
}
