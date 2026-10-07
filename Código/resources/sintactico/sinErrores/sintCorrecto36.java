///[SinErrores]
// Metodo de instancia generico con retorno "void": <TipoMetodo> cubre tanto
// un tipo real como 'void', asi que la misma rama de <CuerpoMiembro> que
// dispara con "<" sirve para ambos sin una alternativa aparte.
class Impresora{
    <T> void imprimir(T x){
    }
}
