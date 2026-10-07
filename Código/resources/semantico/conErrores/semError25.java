///[ErrorSem:ERR_HERENCIA_PERMITIDA_SIN_MODIFICADOR|9]
// Forma es sealed interface que permite a Redonda, pero Redonda se
// declara 'interface' lisa -- la exhaustividad real de Java exige
// sealed/nonsealed tambien para interfaces permitidas.
sealed interface Forma permits Redonda {
    int area();
}

interface Redonda extends Forma {
    int radio();
}
