package Model;

public interface Chequeable {
    // 'tabla' es el sumidero de errores (TablaSimbolos.agregarError) -- cada
    // entidad delega en lo que contiene, pasando la misma tabla hacia abajo.
    void estaBienDeclarado(TablaSimbolos tabla);
}
