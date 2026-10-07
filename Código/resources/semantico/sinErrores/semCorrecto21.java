///[SinErrores]
// Dos interfaces con la misma forma de metodo generico (cada una devuelve su
// propio parametro de tipo), pero con nombres distintos (T vs U) -- deben
// reconocerse como retorno compatible, no como ERR_METODO_RETORNO_INCOMPATIBLE.
interface Fabrica1{
    <T> T crear(T semilla);
}

interface Fabrica2{
    <U> U crear(U semilla);
}

class FabricaDoble implements Fabrica1, Fabrica2{
    <T> T crear(T semilla){
        return semilla;
    }
}
