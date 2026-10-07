///[Error:<|6]
// La genericidad propia de un metodo va ANTES del tipo de retorno
// ("static <T> T m(){}"), nunca despues ("static T <T> m(){}" es error: "T"
// ya se consume como tipo de retorno y lo que sigue, "<", no es idMetVar).
class Caja{
    static T <T> metodo(){
        return null;
    }
}
