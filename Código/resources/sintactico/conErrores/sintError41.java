///[Error:5|11]
// Ternario (REQ-AS-013) sin ":": tras el <valorTrue> se espera "dosPuntos"
// antes del <valorFalse>, no otra expresion pegada.
class TernarioFaltaColon{

    static void metodo()
    {
        boolean flag;
        int a;
        int x;
        x = flag ? a 5;
    }

}
