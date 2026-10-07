package Model.semantico;

import Model.lexico.Token;

import java.util.Set;

public class Parametro implements Chequeable{
    private Token nombre;
    private Tipo tipo;

    public Parametro(Token nombre, Tipo tipo) {
        this.nombre = nombre;
        this.tipo = tipo;
    }

    public Token getNombre(){
        return nombre;
    }

    public Tipo getTipo(){
        return tipo;
    }

    @Override
    public void estaBienDeclarado(TablaSimbolos tabla, Set<String> entornoGenerico){
        tipo.estaBienDeclarado(tabla, entornoGenerico);
    }
}
