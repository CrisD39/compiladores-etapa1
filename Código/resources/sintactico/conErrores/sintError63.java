///[Error:>|6]
// Notacion diamante en la declaracion de un MIEMBRO (atributo): igual que en
// variable local (sintError34) y for-each (sintError35), el diamante solo es
// valido al instanciar con "new" (REQ-AS-010).
class Foo{
    Caja<> campo;
}
