///[SinErrores]
// Shadowing permitido: la clase es generica en T y el metodo vuelve a
// declarar su propio T -- el entorno vigente para la firma del metodo es la
// union (clase T, metodo T), asi que no hay error por "redeclarar" T.
class Caja<T>{
    <T> T metodo(T x){
        return x;
    }
}
