///[ErrorSem:ERR_TIPO_NO_GENERICO|8]
// Logro 5: "Item" no es generica (no declara parametro de tipo) pero se usa
// con argumento -- como el "type Item does not take parameters" de javac.
class Item{
}

class Contenedor{
    Item<Item> elemento;
}
