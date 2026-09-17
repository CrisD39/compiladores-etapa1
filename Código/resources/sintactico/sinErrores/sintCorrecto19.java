///[SinErrores]
// Operador postfijo ++/-- (REQ-AS-014): se engancha en <ExpresionBasica>, no
// una sola vez en <ExpresionCompuesta>, para que aplique a CADA termino de la
// cadena binaria (no solo al primero) y tambien dentro de una expresion
// parentizada que arranca con idMetVar (bug real que aparecio en
// trasParenId(), igual que paso con el ternario).
class PostfijoOk{

    static void metodo()
    {
        int a = 1;
        int b = 2;
        int x;
        boolean flag = true;

        x = a++;
        x = a + b++;
        x = a++ + b--;
        x = (a++);
        x = (a++ + b);
        x = a.b.c++;
        x = flag ? a++ : b--;

        for(int i = 0; i < 10; i++)
        {
            a = a + i;
        }
    }

}
