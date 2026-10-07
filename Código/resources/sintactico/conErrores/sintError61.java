///[Error:,|6]
// Mismo limite que sintError48 pero a nivel metodo: <GenericidadOpcional>
// es la misma produccion para clase, interfaz y metodo, con un unico
// parametro de tipo (REQ-AS-010). Tras "T" se espera ">", no ",".
class Caja{
    <T, U> T metodo(T x){
        return x;
    }
}
