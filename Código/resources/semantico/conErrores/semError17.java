///[ErrorSem:ERR_ATRIBUTO_DUPLICADO|10]
// Atributo redeclarado en una subclase con el mismo nombre que uno heredado.
class Base{

    int x;
}

class Derivada extends Base{

    int x;
}
