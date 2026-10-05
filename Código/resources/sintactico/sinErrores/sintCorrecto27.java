///[SinErrores]
// "while" anidado y "return" en sus tres formas (sin expresion, con
// expresion simple, con expresion booleana compuesta), mezclando literales
// booleanos y de caracter.
class Iteracion{

    boolean activo(){
        return true;
    }

    void esperar(){
        while (activo()){
            while (false){
            }
        }
        return;
    }

    int contar(int limite){
        int i;
        i = 0;
        while (i < limite && activo() || i == 0){
            i = i + 1;
        }
        return i;
    }

    char primero(char c){
        if (c == 'a'){
            return 'z';
        }
        return c;
    }

}
