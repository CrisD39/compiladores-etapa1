// Modo pánico (REQ-AS-008): recuperación cruzando el límite de un método (el
// "}" de cierre de metodoA es de sincronización) y, en un método distinto, un
// token que no puede arrancar ninguna sentencia ("%"). El resto de metodoB
// tiene que seguir parseándose bien después de recuperarse del primer error.
class Panico1 {

    static void metodoA() {
        int x = 5
    }

    static void metodoB() {
        %;
        int y = 1;
    }

}
