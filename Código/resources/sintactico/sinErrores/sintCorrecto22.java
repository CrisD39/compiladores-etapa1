///[SinErrores]
// Interfaz generica con extends de otra interfaz (<GenericidadOpcional> y
// <ExtensionOpcional> juntas). Clase que la implementa (<HerenciaOpcional>
// rama "implements") y clase que extiende otra clase (rama "extends"),
// cada una por separado: HerenciaOpcional es una sola de las dos, no ambas.
interface Base{

    void base();

}

interface ComparableFig<T> extends Base{

    int comparar(T otro);

}

class Circulo implements ComparableFig<Circulo>{

    int comparar(Circulo otro){
        return 0;
    }

    void base(){

    }

}

class CirculoDerivado extends Circulo{

}
