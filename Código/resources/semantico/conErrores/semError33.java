///[ErrorSem:ERR_INTERFAZ_DUPLICADA|8]
// 'implements Uno, Uno' repite la misma interfaz en la lista -- debe ser
// error, no deduplicarse en silencio (Token no sobrescribe equals(), asi
// que el LinkedHashSet no lo detecta solo).
interface Uno {
}

class Malo implements Uno, Uno {
}
