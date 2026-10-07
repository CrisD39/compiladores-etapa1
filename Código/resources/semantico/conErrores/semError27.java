///[ErrorSem:ERR_HERENCIA_NO_PERMITIDA|9]
// Forma es sealed interface que permite solo a Circulo; Cuadrado
// (final, pero no listada en el permits) intenta implementarla -- mismo
// chequeo que para 'extends', ahora del lado 'implements'.
sealed interface Forma permits Circulo {
    int area();
}

final class Cuadrado implements Forma {
    int area(){
        return 1;
    }
}
