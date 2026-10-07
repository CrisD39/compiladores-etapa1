///[SinErrores]
// Alfa y Beta exigen la misma firma Y el mismo tipo de retorno
// ("int getValor()") -- no hay conflicto, Gama los satisface a ambas con
// una sola implementacion.
interface Alfa {
    int getValor();
}

interface Beta {
    int getValor();
}

class Gama implements Alfa, Beta {
    int getValor(){
        return 1;
    }
}
