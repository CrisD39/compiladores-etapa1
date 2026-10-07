package Model.semantico;

import Model.lexico.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

// No implementa Chequeable: no es una declaración que se chequea, es el
// contexto/sumidero de errores que las declaraciones usan para reportar
// (ver Clase/Interfaz/Metodo/Constructor.estaBienDeclarado(TablaSimbolos)).
public class TablaSimbolos {
    private LinkedHashMap<String, EntidadDeclarada> clases;
    private Clase claseActual;
    private Metodo metodoActual;
    private final List<ErrorSemantico> errores = new ArrayList<>();
    // Nombres que ya dispararon ERR_CLASE_DUPLICADA alguna vez. Sin esto,
    // una TERCERA declaración del mismo nombre no colisionaría con nada
    // (la segunda ya removió la entrada de 'clases') y se insertaría limpia
    // -- "resucitando" un nombre que el logro pide descartar para siempre
    // en cuanto se detecta la repetición, sin importar cuántas veces más
    // se repita.
    private final Set<String> nombresInvalidados = new HashSet<>();

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
        if (clases.containsKey(nombre) || nombresInvalidados.contains(nombre)) {
            clases.remove(nombre);
            nombresInvalidados.add(nombre);
            agregarError(new ErrorSemantico("ERR_CLASE_DUPLICADA", entidad.getLinea(), nombre));
            return false;
        }
        clases.put(nombre, entidad);
        return true;
    }

    public EntidadDeclarada buscarClase(String nombre) {
        return clases.get(nombre);
    }
    // Clase e Interfaz comparten namespace (ver insertarClase / analizador_
    // semantico.md), así que se resuelven contra el mismo mapa.
    public EntidadDeclarada buscarInterfaz(String nombre) {
        return clases.get(nombre);
    }
    // Parchean el placeholder dejado por crearMetodo() (Estrategia A de
    // transición, ver "Estrategia de construcción" en analizador_semantico.md)
    // delegando sobre metodoActual, igual criterio que
    // agregarMetodoActualAClaseActual delega sobre claseActual.
    public void setTipoRetornoAMetodoActual(Tipo tipoRetorno) {
        metodoActual.setTipoRetorno(tipoRetorno);
    }

    public void setParametrosAMetodoActual(List<Parametro> parametros) {
        metodoActual.setParametros(parametros);
    }

    public void setParametrosTipoAMetodoActual(List<Token> parametrosTipo) {
        metodoActual.setParametrosTipoPropios(parametrosTipo);
    }

    // Recién ahora -- después de setParametrosAMetodoActual, con los
    // parámetros reales ya parcheados -- se puede registrar en
    // Clase.metodos sin que Clase.agregarMetodo() calcule la firma
    // (nombre + tipos de parámetro) sobre el placeholder vacío. Antes esto
    // se hacía dentro de crearMetodo(), con params=[] todavía, y la firma
    // quedaba fijada mal para siempre (ver el comentario en crearMetodo()
    // en AnalizadorSintacticoImpl.java).
    public void agregarMetodoActualAClaseActual(){
        if (claseActual == null) {
            return;
        }
        claseActual.agregarMetodo(metodoActual);
    }

    // Chequeo de corrección (ver "TODO — Chequeo de corrección" en
    // analizador_semantico.md): corre DESPUÉS de que Pasada 1+2 ya armaron
    // toda la tabla, en dos pasos.
    public void consolidar() {
        // 1) Resolver herencia/ciclos e interfaces -- recorre en orden de
        // declaración (LinkedHashMap) para que, ante un ciclo, la clase/
        // interfaz reportada sea determinística.
        for (EntidadDeclarada entidad : clases.values()) {
            if (entidad instanceof Clase) {
                ((Clase) entidad).consolidar();
            } else if (entidad instanceof Interfaz) {
                ((Interfaz) entidad).consolidar();
            }
        }
        // 2) Con la herencia ya resuelta, chequear el resto de cada
        // declaración (parámetros duplicados, tipos no declarados, etc.),
        // delegando hacia abajo.
        for (EntidadDeclarada entidad : clases.values()) {
            if (entidad instanceof Chequeable) {
                ((Chequeable) entidad).estaBienDeclarado(this, Collections.emptySet());
            }
        }
    }
}
