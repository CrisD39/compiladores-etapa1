///[SinErrores]
// Circulo implementa Forma y define area() -- el chequeo de metodo sin
// implementar debe pasar limpio. Complementa el positivo de multiples
// interfaces (semCorrecto10/11) enfocandose en que los metodos SI estan
// todos presentes.
interface Forma {
    int area();
}

class Circulo implements Forma {
    int area(){
        return 1;
    }
}
