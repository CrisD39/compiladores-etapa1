///[SinErrores]
// Subclase redefine un metodo generico heredado renombrando su propio
// parametro de tipo (T -> U) -- debe reconocerse como la MISMA firma
// (override), no como dos sobrecargas distintas.
class Utilidades{
    <T> T identidad(T x){
        return x;
    }
}

class OtrasUtilidades extends Utilidades{
    <U> U identidad(U x){
        return x;
    }
}
