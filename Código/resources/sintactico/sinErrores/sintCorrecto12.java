///[SinErrores]
// "for" clasico (REQ-AS-009) con las tres secciones completas, en variantes:
// tipo primitivo, idGen, idClase con llamada al constructor, e inicializacion
// por asignacion a una variable ya existente (no declaracion).

class ForClasicoOk{

    static void metodo(int n)
    {
        for (int i = 0; i < n; i = i + 1)
        {
            var x = i;
        }

        for (T t = null; t != null; t = null)
        {
        }

        for (Contador c = new Contador(); c.activo(); c.avanzar())
        {
        }

        for (n = 0; n < 10; n = n + 1)
        {
            var x = n;
        }
    }

}
