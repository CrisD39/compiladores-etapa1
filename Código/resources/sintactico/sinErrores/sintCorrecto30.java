///[SinErrores]
// Operadores unarios prefijos ("+ - !") combinados con acceso a arreglo
// (atributo, no variable local -- ver d24/limite de <RestoDeclLocal>), char
// y parentesis, y precedencia de "!" sobre "&&"/"||" en una condicion de "if".
class Unarios{

    int[] datos;

    void metodo(){
        int x;
        boolean flag;

        x = -datos[0];
        x = -(-x);
        flag = !flag;

        if (!flag && x > 0 || !(x < 0)){
            x = +x;
        }
    }

}
