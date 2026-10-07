///[SinErrores]
// Metodo STATIC generico: misma resolucion que uno de instancia, solo que
// ademas es estatico -- confirma que metodoConGenericidadPropia(true, ...)
// tambien parchea parametrosTipoPropios correctamente.
class Utilidades{
    static <T> T identidad(T x){
        return x;
    }
}
