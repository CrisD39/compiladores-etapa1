///[SinErrores]
// Metodo de INSTANCIA (sin 'static') con su propio parametro de tipo, usado
// como tipo de parametro y de retorno a la vez. Dispara con "<" directo
// (sin 'static' antes) al inicio de <CuerpoMiembro>.
class Caja{
    <T> T envolver(T x){
        return x;
    }
}
