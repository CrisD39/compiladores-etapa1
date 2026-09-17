///[Error:,|7]
// Lista separada por coma en la inicializacion: prohibido (REQ-AS-007), cada
// seccion del "for" admite una sola sentencia/expresion.
class Foo{
    static void metodo()
    {
        for (int i = 0, j = 0; i < 10; i = i + 1)
        {
        }
    }
}
