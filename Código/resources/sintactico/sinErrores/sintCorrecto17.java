///[SinErrores]
// Operador ternario (REQ-AS-013): se engancha en <ExpresionCompuesta>, no en
// <ExpresionBasica>, para que la condicion sea toda la cadena binaria (menos
// precedencia que cualquier operador binario, como en Java). "flag ? 1 : 2+3"
// tiene que leerse "flag ? 1 : (2+3)" y "1 + flag ? a : b" como
// "(1+flag) ? a : b" -- ninguno de los dos se corta a mitad de la cadena.
class TernarioOk{

    static void metodo()
    {
        boolean flag = true;
        int a = 1;
        int b = 2;
        int x;
        int y;

        x = flag ? 1 : 2 + 3;
        y = 1 + flag ? a : b;
        x = (a > b) ? a : b;
    }

}
