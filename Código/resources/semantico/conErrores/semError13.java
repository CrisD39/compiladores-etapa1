///[ErrorSem:ERR_PARAMETRO_DUPLICADO|8]
// Mismo chequeo que semError12.java, pero en la lista de parametros de un
// CONSTRUCTOR en vez de un metodo -- <ArgsFormales> es el mismo no terminal
// en los dos contextos, conviene un caso dedicado para confirmar que el
// chequeo no se salteo uno de los dos.
class Figura{

    Figura(int x, int x){

    }

}
