///[ErrorSem:ERR_HERENCIA_PERMITIDA_SIN_MODIFICADOR|9]
// Figura permite a Circulo, pero Circulo se declara 'class' lisa -- la
// exhaustividad real de Java exige final/sealed/nonsealed, no alcanza con
// estar en el permits.
sealed class Figura permits Circulo {

}

class Circulo extends Figura{

}
