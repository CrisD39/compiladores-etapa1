package Model.semantico;

import Model.lexico.Token;

import java.util.Set;

public class Atributo implements Chequeable {
    private Token nombre;
    private Tipo tipo;

    public Atributo(Token nombre, Tipo tipo) {
        this.nombre = nombre;
        this.tipo = tipo;
    }

    public Token getNombre() {
        return nombre;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public String getFirma() {
        return nombre.getLexema();
    }

    @Override
    public void estaBienDeclarado(TablaSimbolos tabla, Set<String> entornoGenerico) {
        tipo.estaBienDeclarado(tabla, entornoGenerico);
    }
}
