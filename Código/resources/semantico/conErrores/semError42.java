///[ErrorSem:ERR_METODO_REDEFINE_FINAL|13]
// Superclase declara un metodo generico FINAL; la subclase intenta
// redefinirlo renombrando su propio parametro de tipo (T -> U). Antes del
// fix de canonicalizacion, "m(T)" y "m(U)" no coincidian como firma y este
// caso no daba ningun error -- ahora si debe detectarse como redefinicion.
class Base{
    final <T> T m(T x){
        return x;
    }
}

class Derivada extends Base{
    <U> U m(U x){
        return x;
    }
}
