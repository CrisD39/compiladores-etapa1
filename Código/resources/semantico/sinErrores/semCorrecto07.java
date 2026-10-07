///[SinErrores]
// Forma es sealed interface que permite a Redonda; Redonda se declara
// nonsealed, satisfaciendo la exhaustividad -- mismo patron que las
// clases (semCorrecto04/05/06), ahora sobre interfaces.
sealed interface Forma permits Redonda {
    int area();
}

nonsealed interface Redonda extends Forma {
    int radio();
}
