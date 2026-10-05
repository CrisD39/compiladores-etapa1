///[Error:[|12]
// Limite real de la gramatica actual: <RestoDeclLocal> no incluye
// <DimensionesOpcionales> (a diferencia de <CuerpoMiembro>, que si la tiene
// para atributos), asi que una variable LOCAL de tipo arreglo no se puede
// declarar todavia -- mismo alcance ya anotado para <ClausulasFor> en
// "Factorización de <For>". "int[] x;" dentro de un metodo falla apenas
// se ve el "[": <RestoDeclLocal> espera "idMV" (el nombre) justo despues
// del tipo.
class LocalArregloNoSoportado{

    void metodo(){
        int[] x;
    }

}
