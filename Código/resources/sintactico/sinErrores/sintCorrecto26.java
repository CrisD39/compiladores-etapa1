///[SinErrores]
// Encadenado largo de <ReferenciaResto> mezclando "." (var/metodo), "[ ]"
// y llamada estatica como arranque, para estresar la recursion a derecha
// que reemplazo la recursion a izquierda original de <Referencia>.
class Encadenado{

    void metodo(){
        int x;
        Fabrica.crear().obtener().datos[0].siguiente().valor = 1;
        x = this.hijo.lista[0].total();
    }

}

class Fabrica{

    static Fabrica crear(){
        return this;
    }

}
