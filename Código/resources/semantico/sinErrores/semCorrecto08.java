///[SinErrores]
// Caso basico: Forma es sealed interface que permite a Circulo; Circulo se
// declara final al implementarla -- satisface la exhaustividad igual que
// si fuera 'extends' de una clase sealed.
sealed interface Forma permits Circulo {
    int area();
}

final class Circulo implements Forma {
    int area(){
        return 1;
    }
}
