///[Error:final|5]
// Mismo caso que sintError57 pero con 'final' repetido -- el final real
// se recupera solo y 'class Figura {}' se termina parseando sin errores
// adicionales (a diferencia de combinar sealed+final, que sigue roto).
final final class Figura {

}
