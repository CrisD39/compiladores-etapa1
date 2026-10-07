///[SinErrores]
// Figura es sealed y permite solo a Circulo. Circulo es nonsealed, asi que
// cualquier clase puede extenderlo libremente (el chequeo de "permits" es
// de UN SOLO NIVEL: no se propaga a los hijos de una clase permitida que
// a su vez es nonsealed). CirculoRelleno extiende Circulo y no figura en
// ningun "permits" -- no debe fallar.
sealed class Figura permits Circulo {

    int lados;
}

nonsealed class Circulo extends Figura{

}

class CirculoRelleno extends Circulo{

}
