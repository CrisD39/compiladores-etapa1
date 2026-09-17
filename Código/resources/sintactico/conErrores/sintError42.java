///[Error:;|11]
// Ternario (REQ-AS-013) sin <valorFalse>: tras el ":" se espera una
// expresion, no el ";" de cierre de sentencia.
class TernarioFaltaValorFalse{

    static void metodo()
    {
        boolean flag;
        int a;
        int x;
        x = flag ? a : ;
    }

}
