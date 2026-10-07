///[SinErrores]
// Pato implementa dos interfaces sealed (Volador/Nadador, ambas lo
// permiten) mas una interfaz comun (Comun) -- lista de 'implements' con
// tres entradas, mezclando sealed y no-sealed, todas satisfechas.
sealed interface Volador permits Pato {
    void volar();
}

sealed interface Nadador permits Pato {
    void nadar();
}

interface Comun {
    void comun();
}

final class Pato implements Volador, Nadador, Comun {
    void volar(){
    }
    void nadar(){
    }
    void comun(){
    }
}
