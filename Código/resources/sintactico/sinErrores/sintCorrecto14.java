///[SinErrores]
// Genericos anidados y notacion diamante (REQ-AS-010). El anidado funciona en
// cualquier contexto que ya usaba <TipoGenericoOpcional> (atributo, variable
// local clasica, for-each); el diamante solo es valido al instanciar con
// "new". Incluye anidado triple (fuerza el cierre ">>>", tres OP_MAYOR
// consecutivos) y "new" sin ningun genérico (tipo crudo, regresion).

class GenericosOk{

    Caja<Lista<Item>> caja;
    Caja<Lista<Mapa<Item>>> cajaTriple;

    static void metodo()
    {
        Lista<Item> l;
        Caja<Item> c = new Caja<>();
        Mapa<Lista<Item>> m = new Mapa<>();
        Caja<Lista<Item>> c2 = new Caja<Lista<Item>>();
        Caja<Item> raw = new Caja();

        for (Lista<Item> l2 : cajas)
        {
        }
    }

}
