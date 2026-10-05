// Multiples errores de declaracion repetida en un mismo archivo, de
// categorias DISTINTAS (clase, atributo, parametro): confirman que el
// analisis no corta en el primero (Logro 4) y que las categorias no se
// pisan entre si. Se esperan los tres codigos en la misma corrida:
// ERR_CLASE_DUPLICADA, ERR_ATRIBUTO_DUPLICADO y ERR_PARAMETRO_DUPLICADO.
// Lineas exactas en la tabla de propuesta_casos_test_semantico.md.
class Figura{}
class Figura{}

class Caja{

    int lado;
    int lado;

    void mover(int x, int x){

    }

}
