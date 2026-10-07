///[ErrorSem:ERR_METODO_RETORNO_INCOMPATIBLE|13]
// Dos interfaces con metodos genericos de la MISMA forma (mismo nombre,
// mismo parametro de tipo propio canonicalizado), pero con retorno
// REALMENTE incompatible: una devuelve su propio T, la otra devuelve
// literalmente "int" (no un parametro de tipo). Debe seguir detectandose
// como incompatible despues del fix de canonicalizacion -- no se volvio
// permisivo de mas.
interface Fabrica1{
    <T> T crear(T semilla);
}

interface Fabrica2{
    <U> int crear(U semilla);
}

class FabricaRota implements Fabrica1, Fabrica2{
    <T> T crear(T semilla){
        return semilla;
    }
}
