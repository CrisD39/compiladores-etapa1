///[ErrorSem:ERR_TIPO_GENERICO_NO_DECLARADO|10]
// Fuga de scope ENTRE METODOS de la misma clase: el T propio de m1 no esta
// vigente en m2 -- cada Metodo.estaBienDeclarado construye su propio
// entornoVigente a partir del entornoContenedor (la clase), no comparte
// nada con otros metodos hermanos.
class Utilidades{
    <T> T m1(T x){
        return x;
    }
    T m2(){
        return null;
    }
}
