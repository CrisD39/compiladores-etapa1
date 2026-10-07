///[SinErrores]
// Genericos anidados (REQ-AS-010) en las tres posiciones de declaracion de
// un miembro: tipo de atributo, de parametro y de retorno -- con la rama
// idClase de <InstanciadoOParametrico> recursando, y un idGen como argumento.
// La notacion diamante solo aparece al instanciar con "new".
class Caja<T>{
    Caja<Lista<Item>> anidado;
    Lista<T> elementos;

    Caja<Lista<Item>> obtener(Lista<Caja<T>> origen){
        Caja<Lista<Item>> c = new Caja<>();
        return c;
    }
}
