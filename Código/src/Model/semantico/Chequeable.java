package Model.semantico;

import java.util.Set;

public interface Chequeable {
    // 'tabla' es el sumidero de errores (TablaSimbolos.agregarError) -- cada
    // entidad delega en lo que contiene, pasando la misma tabla hacia abajo.
    // 'entornoGenerico' son los lexemas de idGen vigentes en este punto
    // (parámetros de tipo de la clase/interfaz contenedora, unidos a los del
    // método si aplica) -- cada entidad lo propaga o lo extiende antes de
    // delegar hacia lo que contiene.
    void estaBienDeclarado(TablaSimbolos tabla, Set<String> entornoGenerico);
}
