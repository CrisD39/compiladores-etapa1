package Model.semantico;

import Model.lexico.Token;

public class Atributo implements Chequeable {
    private Token nombre;
    private Token tipo;

    public Atributo(Token nombre, Token tipo) {
        this.nombre = nombre;
        this.tipo = tipo;
    }

    public Token getNombre() {
        return nombre;
    }

    public Token getTipo() {
        return tipo;
    }

    public String getFirma() {
        return nombre.getLexema();
    }

    @Override
    public void estaBienDeclarado(TablaSimbolos tabla) {
        if (!((nombre == null) && (tipo == null))) {
            //TODO: reportar error
        }
    }
}
