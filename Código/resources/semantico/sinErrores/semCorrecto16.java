///[SinErrores]
// Metodo generico en una clase NO generica: su T resuelve contra su propio
// entorno (parametrosTipoPropios), no contra el de la clase (que esta vez
// esta vacio). Usado como tipo de parametro y de retorno.
class Utilidades{
    <T> T identidad(T x){
        return x;
    }
}
