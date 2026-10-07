///[ErrorSem:ERR_HERENCIA_NO_PERMITIDA|9]
// Forma es sealed interface que permite solo a Redonda; Cuadrada (sealed
// tambien, pero no listada en el permits de Forma) intenta extenderla --
// mismo chequeo de un solo nivel que ya existe para clases.
sealed interface Forma permits Redonda {
    int area();
}

sealed interface Cuadrada extends Forma permits Nada {
    int lado();
}
