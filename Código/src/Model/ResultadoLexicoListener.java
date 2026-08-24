package Model;

// Permite que AnalizadorLexico entregue tokens y errores a medida que los reconoce
// (streaming), en vez de acumular todo en una lista antes de devolver el control.
// Esto es lo que permite imprimir "de a uno" (REQ-MP-04) sin cargar el archivo en memoria.
public interface ResultadoLexicoListener {

    void onToken(Token token);

    void onError(ErrorLexico error);
}
