package Model.semantico;

import Model.lexico.Token;

public class Parametro implements Chequeable{
    private Token nombre;
    private Token tipo;

    public Parametro(Token nombre, Token tipo) {
        this.nombre = nombre;
        this.tipo = tipo;
    }

    public Token getNombre(){
        return nombre;
    }

    public Token getTipo(){
        return tipo;
    }

    @Override
    public void estaBienDeclarado(TablaSimbolos tabla){
        if(!((nombre != null) && (tipo != null))){
            //nada por ahora
        }
    }
}
