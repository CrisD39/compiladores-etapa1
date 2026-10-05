///[SinErrores]
// Autoreferencia por tipo de ATRIBUTO (no por "extends"): Nodo no hereda de
// si misma, solo tiene un campo de su propio tipo (lista enlazada). No debe
// confundirse con un ciclo de herencia -- el grafo que hay que chequear por
// ciclos es el de "extends"/"implements", no el de tipos de atributo.
class Nodo{

    Nodo siguiente;

}
