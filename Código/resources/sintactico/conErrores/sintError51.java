///[Error:return|7]
// "return" no admite otro "return" como expresion: tras "return" viene
// <ExpresionOpcional>, y "return" no esta en FIRST(<Expresion>) ni es ";".
class ReturnMalo{

    int metodo(){
        return return 1;
    }

}
