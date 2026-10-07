///[ErrorSem:ERR_METODO_INTERFAZ_NOIMPLEMENTADO|6]
// Circulo implementa Forma pero no define area() -- el error se reporta
// sobre la declaracion del metodo EN LA INTERFAZ, no sobre Circulo (ya
// estaba implementado, solo nunca disparaba por el gap de Interfaz.metodos).
interface Forma {
    int area();
}

class Circulo implements Forma {
}
