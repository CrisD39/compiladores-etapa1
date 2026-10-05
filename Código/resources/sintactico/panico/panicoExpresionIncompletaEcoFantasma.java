// Modo pánico (REQ-AS-008), bug encontrado en testing exploratorio (no un
// caso simulado): sincronizar() se come el ";" cuando el token ofensivo ya
// es de sincronización, pensado para el caso "otro error independiente
// justo despues" (ver panicoDosErroresAdyacentes.java). Pero cuando la
// expresion rota termina justo ANTES del ";" que la propia produccion
// (<VarLocal> ";", <Sentencia> ::= <Expresion> ";", etc.) todavia necesita
// para su propio match(PUNTO_COMA), ese ";" no delimitaba un segundo
// problema: era EL MISMO error. sincronizar() se lo come igual, y el
// match(PUNTO_COMA) de mas arriba falla contra el primer token de la
// sentencia siguiente -- que esta perfectamente bien escrita. Un solo
// error real termina reportando dos.
class PanicoEcoFantasma {

    static void metodoA() {
        int x = 1 + ;
        int y;
        y = 2;
    }

}
