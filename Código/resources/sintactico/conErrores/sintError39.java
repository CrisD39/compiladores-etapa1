///[Error:{|8]
// No se puede mezclar tamaño e inicializador (igual que en Java): el corchete
// con "3" adentro ya compromete a la forma de tamaño, "{...}" no tiene
// produccion que lo consuma ahi.
class Foo{
    static void metodo()
    {
        var a = new int[3]{1, 2, 3};
    }
}
