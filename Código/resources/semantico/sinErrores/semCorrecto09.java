///[SinErrores]
// Forma es sealed interface que permite a Circulo. Circulo es nonsealed
// SOLO por 'implements' (no tiene ningun 'extends') -- alcanza con un
// supertipo directo sealed, sin importar si es por extends o implements.
sealed interface Forma permits Circulo {
    int area();
}

nonsealed class Circulo implements Forma {
    int area(){
        return 1;
    }
}
