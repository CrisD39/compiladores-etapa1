///[SinErrores]
// Metodo generico declarado en una interfaz: <MetodoInterfaz> ::=
// <GenericidadOpcional> <TipoMetodo> idMetVar <ArgsFormales> ";".
interface Fabrica{
    <T> T crear(T semilla);
}
