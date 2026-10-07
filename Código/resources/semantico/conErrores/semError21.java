///[ErrorSem:ERR_HERENCIA_CLASE_FINAL|9]
// 'final' en una clase prohibe que cualquiera la extienda, sin excepcion
// (no hay 'permits' que valga). Base es final; Hija no deberia poder
// extenderla.
final class Base {

}

class Hija extends Base{

}
