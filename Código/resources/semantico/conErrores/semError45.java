///[ErrorSem:ERR_PERMITS_NO_ES_SUBTIPO|5]
// "Hijo" esta en el permits de la sealed "Figura", pero NO la extiende --
// Java real exige que todo nombre en permits sea realmente un subtipo
// directo. Gap de Logro 1 ya documentado, antes no se validaba nada.
sealed class Figura permits Hijo{
}

class Hijo{
}
