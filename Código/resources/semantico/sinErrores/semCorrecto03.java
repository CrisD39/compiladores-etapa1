///[SinErrores]
// Composicion mutua entre A y B via atributos (ninguna de las dos usa
// "extends"): cada una tiene un campo del tipo de la otra. Es un ciclo en el
// grafo de TIPOS DE ATRIBUTO, no en el grafo de HERENCIA -- no debe disparar
// ERR_HERENCIA_CICLICA. Mismo espiritu que semCorrecto02, con dos clases en
// vez de una sola.
class A1{

    B1 b;

}

class B1{

    A1 a;

}
