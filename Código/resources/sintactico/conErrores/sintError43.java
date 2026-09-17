///[Error:++|11]
// Prefijo ++ (REQ-AS-014 solo pide postfijo): "++a" no esta en el alcance de
// esta extension. <OperadorUnario> sigue siendo solo "+ | - | !", asi que
// "++" al arrancar una expresion no es un operando valido.
class PostfijoPrefijoFueraDeAlcance{

    static void metodo()
    {
        int a;
        int x;
        x = ++a;
    }

}
