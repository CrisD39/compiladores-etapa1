///[ErrorSem:ERR_NONSEALED_SIN_PADRE_SEALED|6]
// 'nonsealed' sin ningun padre: el modificador no tiene sentido si no hay
// nada que reabrir -- Java real tambien lo rechaza ("modifier 'non-sealed'
// not allowed here"). No hace falta que exista ninguna sealed en este
// archivo para que el chequeo dispare.
nonsealed class Suelta {

}
