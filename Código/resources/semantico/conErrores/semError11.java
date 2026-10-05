///[ErrorSem:ERR_CONSTRUCTOR_DUPLICADO|13]
// Dos constructores con la misma lista de parametros (mismo tipo y mismo
// orden) -- igual que semError10.java, es duplicado sin importar la regla
// de sobrecarga que se adopte mas adelante.
class Punto{

    int x;

    Punto(int x){
        this.x = x;
    }

    Punto(int x){
        this.x = x;
    }

}
