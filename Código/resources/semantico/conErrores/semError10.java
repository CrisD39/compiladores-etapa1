///[ErrorSem:ERR_METODO_DUPLICADO|13]
// Dos metodos con el mismo nombre Y la misma lista de parametros: es
// duplicado bajo CUALQUIER regla de sobrecarga que se termine adoptando (la
// pregunta abierta de propuesta_casos_test_semantico.md es solo sobre
// "mismo nombre, distintos parametros" -- este caso no depende de esa
// decision todavia pendiente).
class Calculadora{

    int sumar(int a, int b){
        return a + b;
    }

    int sumar(int a, int b){
        return a + b;
    }

}
