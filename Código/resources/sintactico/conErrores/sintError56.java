///[Error:,|11]
// Mezcla de expresion y parametro en la misma lista: el primer elemento
// ("x + y") ya descarta que sea una lista de parametros (a diferencia de
// sintError22, donde el malformado esta en el medio de una lista que
// arranca bien); tras reconstruir la expresion "x + y" solo se espera ")".
class LambdaExpresionYParametro{
    static void metodo(){
        int x;
        int y;
        int z;
        var f = (x + y, z) -> x;
    }
}
