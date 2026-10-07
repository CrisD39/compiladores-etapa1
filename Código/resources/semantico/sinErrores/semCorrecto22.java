///[SinErrores]
// Logro 5: idGen como argumento anidado, resuelto contra el entorno vigente
// -- el T de la clase en el atributo, el U propio del metodo en su retorno y
// parametro. "Lista cruda" (raw type, sin <...>) se acepta como en Java.
class Lista<T>{
}

class Caja<T>{
    Lista<T> elementos;
    Lista cruda;

    <U> Lista<U> envolver(Lista<U> origen){
        return origen;
    }
}
