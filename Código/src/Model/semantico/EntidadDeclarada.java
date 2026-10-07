package Model.semantico;

import Model.lexico.Token;

import java.util.List;

public interface EntidadDeclarada {
    String getName();

    // Línea de la declaración (token de nombre) -- para reportar duplicados
    // sobre la entidad que efectivamente dispara el error.
    int getLinea();

    // Parámetro de tipo declarado (0 o 1 elemento) -- para saber si la
    // entidad acepta un argumento genérico al usarla como tipo.
    List<Token> getParametrosTipo();
}
