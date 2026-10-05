///[SinErrores]
// Constructor con visibilidad explicita (idClase como constructor via
// <TrasIdClaseMiembro>) combinado con herencia, atributo de tipo arreglo
// generico y llamada a constructor con new de tipo generico instanciado.
class Lista<T>{

    T[] datos;

    private Lista(int capacidad){

    }

    public static void metodo(){
        Lista<Circulo> lista;
        lista = new Lista<Circulo>(10);
    }

}

class Circulo{

}
