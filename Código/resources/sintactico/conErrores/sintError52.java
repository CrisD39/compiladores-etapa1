///[Error:{|6]
// Lista de 'implements' con coma final sin otra interfaz despues --
// <ListaInterfacesResto> exige otro <TipoReferencia> tras la coma, no se
// puede quedar colgada. Reemplaza el viejo contenido de este archivo:
// "implements A, B" (sin coma colgante) ya es valido ahora (Logro 2).
class Malo implements Uno, {

}

interface Uno{

}
