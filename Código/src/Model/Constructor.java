package Model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Constructor implements Chequeable{
    private Token nombre;
    private List<Parametro> parametros;

    public Constructor(Token nombre, List<Parametro> parametros){
        this.nombre = nombre;
        this.parametros = parametros;
    }

    public Token getNombre() {
        return nombre;
    }

    // Firma para distinguir sobrecargas entre constructores de una misma
    // clase (ver Clase.agregarConstructor) -- igual criterio que
    // Metodo.getFirma(), pero el nombre es siempre el de la clase, así que
    // alcanza con los tipos de parámetro para distinguir.
    public String getFirma() {
        String tipos = parametros.stream()
                .map(p -> p.getTipo().getLexema())
                .collect(Collectors.joining(","));
        return nombre.getLexema() + "(" + tipos + ")";
    }

    @Override
    public void estaBienDeclarado(TablaSimbolos tabla) {
        Set<String> nombresVisibles = new HashSet<>();
        for (Parametro parametro : parametros) {
            if (!nombresVisibles.add(parametro.getNombre().getLexema())) {
                tabla.agregarError(new ErrorSemantico(
                        "ERR_PARAMETRO_DUPLICADO", parametro.getNombre().getLinea(), parametro.getNombre().getLexema()));
            }
        }
    }
}
