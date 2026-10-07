///[SinErrores]
// Hija extiende dos interfaces: Base (sealed, permite a Hija) y Comun
// (normal). Hija se declara nonsealed, satisfaciendo la exhaustividad del
// lado de Base; Comun, al no ser sealed, no exige nada.
sealed interface Base permits Hija {
    void base();
}

interface Comun {
    void extra();
}

nonsealed interface Hija extends Base, Comun {
    void propio();
}
