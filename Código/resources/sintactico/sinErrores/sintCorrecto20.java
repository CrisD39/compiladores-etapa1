///[SinErrores]
// Postfijo (REQ-AS-014) encadenado: "a++--" es sintacticamente valido, igual
// que en Java real. <PostfijoOpcional> es recursiva (++|-- <PostfijoOpcional>
// | ϵ), igual que la gramatica de Java (PostIncrementExpression /
// PostDecrementExpression toman como operando otro PostfixExpression). javac
// tambien acepta "a++--" al parsear y recien lo rechaza en el chequeo de
// tipos ("required: variable, found: value") porque "a++" da un valor, no una
// variable -- esa distincion, igual que con "1++" o "1 = 2;", queda para la
// etapa semantica, no para esta.
class PostfijoEncadenadoOk{

    static void metodo()
    {
        int a = 1;
        int x;
        x = a++--;
        x = a----++;
    }

}
