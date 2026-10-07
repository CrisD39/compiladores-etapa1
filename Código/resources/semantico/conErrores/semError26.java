///[ErrorSem:ERR_NONSEALED_SIN_PADRE_SEALED|5]
// 'nonsealed' en una interfaz sin ningun padre: el modificador no tiene
// sentido si no hay nada que reabrir -- mismo chequeo que para clases
// (ERR_NONSEALED_SIN_PADRE_SEALED), reusado tal cual para Interfaz.
nonsealed interface Suelta {
    int m();
}
