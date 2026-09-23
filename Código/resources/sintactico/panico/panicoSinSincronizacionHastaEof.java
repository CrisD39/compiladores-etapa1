// Modo pánico (REQ-AS-008), caso limite: no hay ningun ";"/"{"/"}" entre el
// error y el fin del archivo. sincronizar() tiene que terminar igual (parar en
// EOF, que el lexico repite para siempre) sin colgarse.
class Panico2 {

    static void metodoA() {
        int x = 5
