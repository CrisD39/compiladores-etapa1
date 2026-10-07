package Model.semantico;

import Model.lexico.Token;

// Wrapper mínimo sobre el Token del tipo. La jerarquía real
// (TipoPrimitivo/TipoReferencia/TipoGenerico/TipoArreglo, ver "Diseño:
// jerarquía Tipo" en analizador_semantico.md) queda pendiente hasta que se
// resuelva idClase/idGen contra la tabla de símbolos — por ahora esto solo
// existe para que el resto del modelo compile.
public class Tipo {
    private Token token;
    //mas adelante podría tener una lista de compatibles.

    public Tipo(Token token) {
        this.token = token;
    }

    public Token getToken() {
        return token;
    }
}
