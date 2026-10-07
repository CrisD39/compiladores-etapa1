///[ErrorSem:ERR_NONSEALED_SIN_PADRE_SEALED|10]
// 'nonsealed' declarado sobre una clase cuyo padre no es sealed -- el
// modificador solo tiene sentido reabriendo una rama que una sealed cerro.
// Base es una clase comun, sin 'sealed', asi que Hija no deberia poder
// declararse nonsealed solo porque extiende de algo.
class Base {

}

nonsealed class Hija extends Base{

}
