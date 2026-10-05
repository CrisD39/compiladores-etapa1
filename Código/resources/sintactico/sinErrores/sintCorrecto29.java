///[SinErrores]
// Multiples clases e interfaces al mismo nivel (<ListaClases> recursiva
// sobre las dos alternativas, Clase e Interfaz, intercaladas) para
// asegurar que la lista de nivel superior no favorece un orden fijo.
interface Uno{

    void a();

}

class Dos implements Uno{

    void a(){

    }

}

interface Tres{

    void b();

}

class Cuatro{

}
