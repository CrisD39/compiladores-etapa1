///[SinErrores]
// Ternario anidado y asociativo a derecha (REQ-AS-013): el <valorFalse> es
// <ExpresionCompuesta>, que ya termina en su propio ternarioOpcional(), asi
// que "a ? b : c ? d : e" sale "a ? b : (c ? d : e)" sin repetir nada en la
// gramatica. Tambien se prueba anidado en el <valorTrue> ("a ? b?c:d : e").
class TernarioAnidadoOk{

    static void metodo()
    {
        int a = 1;
        int b = 2;
        int c = 3;
        int d = 4;
        int e = 5;
        int x;

        x = a > b ? c : d > e ? c : d;
        x = a > b ? (c > d ? c : d) : e;
        x = a > b ? c > d ? c : d : e;
    }

}
