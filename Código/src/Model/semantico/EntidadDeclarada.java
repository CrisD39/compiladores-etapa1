package Model.semantico;

public interface EntidadDeclarada {
    String getName();

    // Línea de la declaración (token de nombre) -- para reportar duplicados
    // sobre la entidad que efectivamente dispara el error.
    int getLinea();
}
