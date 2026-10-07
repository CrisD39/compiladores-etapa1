package Model.semantico;

import Model.lexico.Token;
import Model.lexico.TokenType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Metodo implements Chequeable {
    private Token nombre;
    private Tipo tipoRetorno; // null representa "void" ?? not sure
    private boolean estatico;
    private boolean esFinal;
    private List<Parametro> parametros;
    // Variables locales del cuerpo (ver "Chequeo de sentencias" en
    // analizador_semantico.md): se reutiliza Atributo (nombre+tipo) en vez de
    // una clase nueva, mismo criterio que para Constructor con Metodo. Todavía
    // no se puebla — bloque() sigue siendo puramente sintáctico (Pasada 2).
    private List<Atributo> variablesLocales = new ArrayList<>();
    //No sé como hacer para que el metodo sepa que atributos estan habilitados para utilizar.
    private Clase miClase; //la clase que tengo asociadaaa
    // Parámetros de tipo propios del método (ej. el T de "<T> T metodo(T x)"),
    // distintos del/los de la clase/interfaz contenedora -- ver
    // TablaSimbolos.setParametrosTipoAMetodoActual().
    private List<Token> parametrosTipoPropios = new ArrayList<>();

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

    public void setFinal(boolean esFinal) {
        this.esFinal = esFinal;
    }

    public boolean isFinal() {
        return esFinal;
    }

    public List<Parametro> getParams() {
        return parametros;
    }

    public List<Token> getParametrosTipoPropios() {
        return parametrosTipoPropios;
    }

    public void setParametrosTipoPropios(List<Token> parametrosTipoPropios) {
        this.parametrosTipoPropios = parametrosTipoPropios;
    }

    private int indiceEnPropios(String lexema) {
        for (int i = 0; i < parametrosTipoPropios.size(); i++) {
            if (parametrosTipoPropios.get(i).getLexema().equals(lexema)) {
                return i;
            }
        }
        return -1;
    }

    // Para firma/retorno: un idGen que es parametro de tipo PROPIO de este
    // metodo se identifica por posicion, no por el nombre elegido -- "<T> T
    // m(T x)" y "<U> U m(U x)" deben ser la MISMA firma (mismo metodo a
    // efectos de override), igual que en Java real donde el nombre del type
    // variable propio no importa para la identidad del metodo. Todo lo demas
    // (primitivos, idClase, idGen de la clase/interfaz contenedora) sigue
    // comparandose por lexema crudo.
    private String tipoCanonico(Token tipoToken) {
        if (tipoToken.getTipo() == TokenType.ID_GEN) {
            int indice = indiceEnPropios(tipoToken.getLexema());
            if (indice >= 0) {
                return "#" + indice;
            }
        }
        return tipoToken.getLexema();
    }

    /**
     * Firma para distinguir sobrecargas dentro de una clase (ver
     * Clase.agregarMetodo): nombre + cabeza del tipo de cada parámetro. El
     * argumento genérico se ignora a propósito: m(Lista<A>) y m(Lista<B>)
     * tienen el mismo erasure en Java y no son una sobrecarga válida.
     */
    public String getFirma() {
        String tipos = parametros.stream()
                .map(p -> tipoCanonico(p.getTipo().getToken()))
                .collect(Collectors.joining(","));
        return nombre.getLexema() + "(" + tipos + ")";
    }

    // Retorno canonicalizado -- mismo criterio que getFirma(), usado para
    // comparar compatibilidad de retorno entre metodos con la misma firma
    // (Clase.consolidarInterfaces()). null representa "void", igual que
    // tipoRetorno.
    public String getRetornoCanonico() {
        return tipoRetorno == null ? null : tipoCanonico(tipoRetorno.getToken());
    }

    @Override
    public void estaBienDeclarado(TablaSimbolos tabla, Set<String> entornoContenedor) {
        // Entorno vigente para la firma de ESTE método: el de la clase/
        // interfaz contenedora más los propios -- shadowing permitido (si el
        // método reutiliza un nombre ya vigente, no es error, ver decisión
        // de diseño en analizador_semantico.md).
        Set<String> entornoVigente = new HashSet<>(entornoContenedor);
        for (Token parametroTipo : parametrosTipoPropios) {
            entornoVigente.add(parametroTipo.getLexema());
        }
        if (tipoRetorno != null) {
            tipoRetorno.estaBienDeclarado(tabla, entornoVigente);
        }
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
            parametro.estaBienDeclarado(tabla, entornoVigente);
        }
    }
}
