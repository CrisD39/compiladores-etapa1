///[SinErrores]
// Clase sealed que permite exactamente una subclase por nombre, y esa
// subclase la extiende, declarandose 'final' (exhaustividad: toda subclase
// permitida debe elegir final/sealed/nonsealed, una 'class' lisa ya no
// alcanza aunque figure en el permits) -- el chequeo de "permits" debe pasar.
sealed class Figura permits Circulo {

    int lados;
}

final class Circulo extends Figura{

}
