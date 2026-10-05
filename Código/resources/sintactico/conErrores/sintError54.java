///[Error:->|8]
// El idMetVar era el comienzo de una llamada a metodo SIN cadena previa
// ("x()", no ".metodo()" como en sintError20): tras "( idMetVar" el "("
// que sigue ya descarta que sea parametro (TrasParenId reconstruye la
// expresion via TrasId), y el "->" final no tiene produccion que lo consuma.
class LambdaLlamadaDirecta{
    static void metodo(){
        var f = (x()) -> x();
    }
    static int x(){
        return 1;
    }
}
