// Modo pánico (REQ-AS-008): el caso que expuso el bug de diseño durante la
// validación. "new Foo" sin "(...)" falla justo cuando el token que sigue ya
// es un ";" de sincronización -- sincronizar() no debe descartarlo de más,
// porque si lo hace se traga el error independiente de la línea siguiente.
class Panico3 {

    static void metodoA() {
        Foo x = new Foo;
        int y = ;
    }

}
