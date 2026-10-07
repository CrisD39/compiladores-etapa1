///[ErrorSem:ERR_TIPO_NO_DECLARADO|5]
// "Hijo" en el permits de la sealed "Figura" no corresponde a NINGUNA clase
// declarada -- mismo codigo que un tipo no declarado en cualquier otro
// lado (Clase.validarPermitidos reusa ERR_TIPO_NO_DECLARADO para este caso).
sealed class Figura permits Hijo{
}
