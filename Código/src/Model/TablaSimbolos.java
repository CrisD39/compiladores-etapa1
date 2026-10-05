package Model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

// No implementa Chequeable: no es una declaración que se chequea, es el
// contexto/sumidero de errores que las declaraciones usan para reportar
// (ver Clase/Interfaz/Metodo/Constructor.estaBienDeclarado(TablaSimbolos)).
public class TablaSimbolos {
    private LinkedHashMap<String, EntidadDeclarada> clases;
    private Clase claseActual;
    private Metodo metodoActual;
    private final List<ErrorSemantico> errores = new ArrayList<>();

    public TablaSimbolos() {
        this.clases = new LinkedHashMap<>();
    }

    public void agregarError(ErrorSemantico error) {
        errores.add(error);
    }

    public List<ErrorSemantico> getErrores() {
        return errores;
    }

    public void setClaseActual(Clase actual){
        claseActual = actual;
    }

    public Clase getClaseActual() {
        return claseActual;
    }

    public void setMetodoActual(Metodo actual){
        metodoActual = actual;
    }

    // Comparten namespace (Pasada 1 del EDT): si el nombre ya existe, se
    // descartan AMBAS entidades (Logro 4 -- "nombres repetidos... descarta
    // ambas") y se reporta sobre la línea de la SEGUNDA declaración, la que
    // "ya existe" cuando se intenta registrar.
    public boolean insertarClase(EntidadDeclarada entidad) {
        String nombre = entidad.getName();
        if (clases.containsKey(nombre)) {
            clases.remove(nombre);
            agregarError(new ErrorSemantico("ERR_CLASE_DUPLICADA", entidad.getLinea(), nombre));
            return false;
        }
        clases.put(nombre, entidad);
        return true;
    }

    public EntidadDeclarada buscarClase(String nombre) {
        return clases.get(nombre);
    }


    public void agregarMetodoAClaseActual(Metodo nuevoMetodo){
        if (claseActual == null) {
            System.out.println("No hay clase actual, weird ");
        }
         claseActual.agregarMetodo(nuevoMetodo);
    }

    // Chequeo de corrección (ver "TODO — Chequeo de corrección" en
    // analizador_semantico.md): corre DESPUÉS de que Pasada 1+2 ya armaron
    // toda la tabla, en dos pasos.
    public void consolidar() {
        // 1) Resolver herencia/ciclos -- recorre en orden de declaración
        // (LinkedHashMap) para que, ante un ciclo, la clase/interfaz
        // reportada sea determinística.
        for (EntidadDeclarada entidad : clases.values()) {
            if (entidad instanceof Clase) {
                ((Clase) entidad).consolidar();
            } else if (entidad instanceof Interfaz) {
                ((Interfaz) entidad).consolidar();
            }
        }
        // 2) Con la herencia ya resuelta, chequear el resto de cada
        // declaración (parámetros duplicados, etc.), delegando hacia abajo.
        for (EntidadDeclarada entidad : clases.values()) {
            if (entidad instanceof Chequeable) {
                ((Chequeable) entidad).estaBienDeclarado(this);
            }
        }
    }
}
