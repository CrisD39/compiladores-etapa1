///[Error:sealed|5]
// Combinacion de modificadores (final + sealed + final) en la misma
// declaracion -- cada modificador fuera de lugar se reporta por separado
// con el mismo mensaje claro, y 'class Figura {}' termina parseando bien.
final sealed final class Figura {

}
