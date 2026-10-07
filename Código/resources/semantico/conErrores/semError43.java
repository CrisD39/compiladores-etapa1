///[ErrorSem:ERR_METODO_DUPLICADO|9]
// Misma clase declara dos veces "el mismo" metodo generico, solo que con el
// parametro de tipo propio renombrado (T -> U) -- mismo erasure en Java
// real, no es una sobrecarga valida.
class Caja{
    <T> T m(T x){
        return x;
    }
    <U> U m(U x){
        return x;
    }
}
