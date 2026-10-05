///[Error:implements|5]
// <Interfaz> ::= interface idClase <GenericidadOpcional> <ExtensionOpcional>
// "{" ... "}" -- <ExtensionOpcional> solo admite "extends", nunca
// "implements" (eso es exclusivo de <HerenciaOpcional>, para clases).
interface Malo implements Base{

}

interface Base{

}
