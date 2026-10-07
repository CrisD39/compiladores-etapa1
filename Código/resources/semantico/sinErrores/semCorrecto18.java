///[SinErrores]
// Logro 5: tipos genericos anidados con todas las clases declaradas, en las
// tres posiciones de declaracion (atributo, parametro y retorno). Cada
// nivel del argumento resuelve contra la tabla.
class Item{
}

class Lista<T>{
}

class Caja<T>{
    Caja<Lista<Item>> anidado;

    Lista<Item> convertir(Caja<Lista<Item>> origen){
        return null;
    }
}
