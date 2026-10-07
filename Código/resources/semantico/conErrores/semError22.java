///[ErrorSem:ERR_METODO_REDEFINE_FINAL|11]
// Base declara un metodo final; Hija lo redefine con la misma firma --
// eso es justo lo que 'final' prohibe (chequeo de firmas, no de cuerpo).
class Base {
    final int valor(){
        return 1;
    }
}

class Hija extends Base{
    int valor(){
        return 2;
    }
}
