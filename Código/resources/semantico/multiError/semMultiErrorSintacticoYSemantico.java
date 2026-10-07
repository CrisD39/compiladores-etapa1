// Mezcla de error SINTACTICO y error SEMANTICO en el mismo archivo:
// AnalizadorSemanticoHandlerImpl.analizar() corre tablaSimbolos.consolidar()
// siempre, aun si hubo errores sintacticos, y ModuloPrincipalET3 junta
// ambas listas en la salida. "sealed sealed class Roto" dispara el error
// sintactico de modificador repetido (linea 10) SIN sincronizar -- el
// "sealed" sobrante se vuelve a mirar en la proxima vuelta de listaClases(),
// asi que "class Roto permits Hijo{...}" se termina parseando bien y el
// resto del archivo no se pierde. "NoExiste" en Figura dispara
// ERR_TIPO_NO_DECLARADO (linea 17). Verificado contra la salida real.
sealed sealed class Roto permits Hijo{
}

final class Hijo extends Roto{
}

class Figura{
    NoExiste campo;
}
