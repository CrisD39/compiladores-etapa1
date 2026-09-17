///[SinErrores]
// Sentencia "for" (REQ-AS-007): forma clasica con ";" y for-each con ":".
// Las tres secciones del clasico son opcionales (for(;;), como en Java) y el
// cuerpo es <Sentencia> (con o sin llaves), igual que if/while. Incluye el
// caso en que la inicializacion es una expresion (asignacion a variable
// existente o llamada a metodo estatico), no solo una declaracion.

class ForOk{

    static void metodo(int n)
    {
        for (int i = 0; i < n; i = i + 1)
        {
            var x = i;
        }

        for (;;)
        {
        }

        for (int i = 0; ; i = i + 1)
        {
        }

        for (; n < 10; )
        {
        }

        for (Item it : lista)
        {
        }

        for (Fabrica.reset(); n < 10; n = n + 1)
        {
        }

        for (n = 0; n < 10; n = n + 1)
            n = n + 1;

        for (int i = 0; i < n; i = i + 1)
            for (int j = 0; j < n; j = j + 1)
            {
                var x = i + j;
            }
    }

}
