package Model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Metodo implements Chequeable {
    private Token nombre;
    private Tipo tipoRetorno; // null representa "void" ?? not sure
    private boolean estatico;
    private List<Parametro> parametros;
    // Variables locales del cuerpo (ver "Chequeo de sentencias" en
    // analizador_semantico.md): se reutiliza Atributo (nombre+tipo) en vez de
    // una clase nueva, mismo criterio que para Constructor con Metodo. Todavía
    // no se puebla — bloque() sigue siendo puramente sintáctico (Pasada 2).
    private List<Atributo> variablesLocales = new ArrayList<>();
    //No sé como hacer para que el metodo sepa que atributos estan habilitados para utilizar.
    private Clase miClase; //la clase que tengo asociadaaa


    public Metodo(Token nombre, Tipo tipoRetorno, boolean estatico, List<Parametro> params) {
        this.nombre = nombre;
        this.tipoRetorno = tipoRetorno;
        this.estatico = estatico;
        this.parametros = params;
    }

    public Token getNombre() {
        return nombre;
    }

    // Parchea el placeholder creado por crearMetodo() (Estrategia A de
    // transición, ver "Estrategia de construcción" en analizador_semantico.md)
    // con los valores reales una vez que Pasada 2 los termina de resolver.
    public void setTipoRetorno(Tipo tipoRetorno) {
        this.tipoRetorno = tipoRetorno;
    }

    public void setParametros(List<Parametro> parametros) {
        this.parametros = parametros;
    }

    public Tipo getTipoRetorno() {
        return tipoRetorno;
    }

    public boolean isEstatico() {
        return estatico;
    }

    public List<Parametro> getParams() {
        return parametros;
    }

    /**
     * Firma para distinguir sobrecargas dentro de una clase (ver
     * Clase.agregarMetodo): nombre + tipo de cada parámetro. Solo el lexema
     * del tipo por ahora — Parametro todavía guarda Token crudo, sin
     * arreglos/genéricos resueltos; cuando exista la jerarquía Tipo real este
     * es el único lugar que hay que enriquecer.
     */
    public String getFirma() {
        String tipos = parametros.stream()
                .map(p -> p.getTipo().getLexema())
                .collect(Collectors.joining(","));
        return nombre.getLexema() + "(" + tipos + ")";
    }

    @Override
    public void estaBienDeclarado(TablaSimbolos tabla) {
        // El chequeo de variables locales (variablesLocales) es chequeo de
        // SENTENCIAS, fuera de alcance de esta etapa -- ver
        // analizador_semantico.md. Acá solo se valida que los parámetros no
        // se repitan entre sí.
        Set<String> nombresVisibles = new HashSet<>();
        for (Parametro parametro : parametros) {
            if (!nombresVisibles.add(parametro.getNombre().getLexema())) {
                tabla.agregarError(new ErrorSemantico(
                        "ERR_PARAMETRO_DUPLICADO", parametro.getNombre().getLinea(), parametro.getNombre().getLexema()));
            }
        }
    }
}
