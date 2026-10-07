///[SinErrores]
// Figura es sealed y permite a Poligono. Poligono tambien es sealed (con su
// propio permits) -- es una de las tres opciones validas para una subclase
// permitida (final/sealed/nonsealed), no solo final/nonsealed como en los
// otros casos de control.
sealed class Figura permits Poligono {

}

sealed class Poligono extends Figura permits Triangulo {

}

final class Triangulo extends Poligono{

}
