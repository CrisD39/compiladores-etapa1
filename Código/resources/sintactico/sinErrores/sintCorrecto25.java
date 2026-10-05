///[SinErrores]
// "else" colgante (REQ-AS visto en el analisis LL(1), Paso 4): se resuelve
// por convencion ligando siempre con el "if" mas cercano. Sintacticamente
// las dos formas (if anidado sin llaves seguido de else, e if con bloque
// explicito) tienen que parsear igual de bien.
class DanglingElse{

    void metodo(){
        int a;
        int b;
        a = 1;
        b = 2;

        if (a > 0)
            if (b > 0)
                a = 1;
            else
                a = 2;

        if (a > 0){
            if (b > 0){
                a = 1;
            }
        } else {
            a = 3;
        }
    }

}
