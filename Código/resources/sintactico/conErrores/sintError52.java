///[Error:,|6]
// "implements" (igual que "extends") espera un unico <TipoReferencia>, no
// una lista de interfaces separadas por coma -- "implements A, B" (varias
// interfaces a la vez) no existe en esta gramatica, a diferencia de Java
// real. Complementa a sintError01 (mismo problema, del lado "extends").
class Malo implements Uno, Dos{

}

interface Uno{

}

interface Dos{

}
