///[SinErrores]
// Caso completo de Logro 2 que antes no se podia testear en limpio: Pato
// implementa Volador y Nadador, cada una con un mover() de distinta firma
// (parametro int vs char) -- eso es sobrecarga, no conflicto (la firma ya
// las distingue), y ahora que la sobrecarga real funciona dentro de la
// clase, Pato puede implementar ambas sin disparar ERR_METODO_DUPLICADO
// ni ERR_METODO_INTERFAZ_NOIMPLEMENTADO.
interface Volador {
    void mover(int pasos);
}

interface Nadador {
    void mover(char pasos);
}

class Pato implements Volador, Nadador {
    void mover(int pasos){

    }

    void mover(char pasos){

    }
}
