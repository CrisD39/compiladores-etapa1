///[SinErrores]
// Logro 2: una clase ahora puede implementar varias interfaces a la vez
// (<ListaInterfaces> separada por coma, antes un unico <TipoReferencia>),
// y una interfaz puede extender varias interfaces a la vez (mismo mecanismo
// de lista, del lado <ExtensionOpcional>).
interface Volador{

    void volar();

}

interface Nadador{

    void nadar();

}

interface AnfibioVolador extends Volador, Nadador{

    void saltar();

}

class Pato implements Volador, Nadador{

    void volar(){

    }

    void nadar(){

    }

}
