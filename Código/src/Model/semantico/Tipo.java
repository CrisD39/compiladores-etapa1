package Model.semantico;

import Model.lexico.Token;
import Model.lexico.TokenType;

import java.util.Set;

// Tipo usado en una declaración (atributo, parámetro o retorno). Recursivo:
// el argumento genérico de "idClase<argumento>" es a su vez un Tipo (un idGen
// suelto, u otro idClase con su propio argumento), igual que
// <InstanciadoOParametrico> en la gramática. Un único argumento por nivel
// (REQ-AS-010). La jerarquía completa (TipoPrimitivo/TipoReferencia/
// TipoGenerico/TipoArreglo, ver "Diseño: jerarquía Tipo" en
// analizador_semantico.md) y las dimensiones de arreglo siguen pendientes.
public class Tipo implements Chequeable {
    private final Token token;
    private final Tipo argumento;

    public Tipo(Token token) {
        this(token, null);
    }

    public Tipo(Token token, Tipo argumento) {
        this.token = token;
        this.argumento = argumento;
    }

    public Token getToken() {
        return token;
    }

    public Tipo getArgumento() {
        return argumento;
    }

    // Logro 5: cada nivel se valida contra la tabla (idClase) o contra el
    // entorno de genéricos vigente (idGen). El argumento se recorre aunque
    // la cabeza falle, para reportar todos los errores en la misma corrida
    // (Logro 4). Un raw type (Caja sin <...> siendo genérica) se acepta,
    // como en Java.
    @Override
    public void estaBienDeclarado(TablaSimbolos tabla, Set<String> entornoGenerico) {
        if (token.getTipo() == TokenType.ID_GEN) {
            if (!entornoGenerico.contains(token.getLexema())) {
                tabla.agregarError(new ErrorSemantico(
                        "ERR_TIPO_GENERICO_NO_DECLARADO", token.getLinea(), token.getLexema()));
            }
        } else if (token.getTipo() == TokenType.ID_CLASE) {
            EntidadDeclarada entidad = tabla.buscarClase(token.getLexema());
            if (entidad == null) {
                tabla.agregarError(new ErrorSemantico(
                        "ERR_TIPO_NO_DECLARADO", token.getLinea(), token.getLexema()));
            } else if (argumento != null && entidad.getParametrosTipo().isEmpty()) {
                tabla.agregarError(new ErrorSemantico(
                        "ERR_TIPO_NO_GENERICO", token.getLinea(), token.getLexema()));
            }
        }
        if (argumento != null) {
            argumento.estaBienDeclarado(tabla, entornoGenerico);
        }
    }
}
