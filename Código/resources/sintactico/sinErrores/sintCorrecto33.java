///[SinErrores]
// Metodo static con su propio parametro de tipo, en una clase NO generica:
// "static <T> T identidad(T x){...}". <GenericidadOpcional> va entre 'static'
// y el tipo de retorno, antes de idMetVar.
class Utilidades{
    static <T> T identidad(T x){
        return x;
    }
}
