///[SinErrores]
// Tipos arreglo (<DimensionesOpcionales>) como atributo, parametro y tipo
// de retorno, en una y dos dimensiones, con acceso via <ReferenciaResto>
// ("[ <Expresion> ]") encadenado para el caso 2D. No declara una variable
// LOCAL de tipo arreglo a proposito: <RestoDeclLocal> hoy no admite
// <DimensionesOpcionales> (mismo alcance que <ClausulasFor>, ver
// "Factorización de <For>" en la documentación) -- eso se prueba aparte
// como caso de error.
class Matriz{

    int[] fila;
    int[][] datos;

    int[] procesar(int[] entrada, int n){
        return entrada;
    }

    int elemento(int i, int j){
        return datos[i][j];
    }

    void cargar(){
        fila[0] = 1;
        datos[0][0] = fila[0];
    }

}
