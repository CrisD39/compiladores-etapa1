///[SinErrores]
// Shadowing: la clase es generica en T y el metodo vuelve a declarar su
// propio T. A nivel sintactico esto se acepta sin mas -- el chequeo de
// shadowing (permitido, ver analizador_semantico.md) es semantico.
class Caja<T>{
    <T> T metodo(T x){
        return x;
    }
}
