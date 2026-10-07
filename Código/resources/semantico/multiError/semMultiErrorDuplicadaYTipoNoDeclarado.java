// Nombre de clase duplicado ("Figura") descarta AMBAS entidades (Logro 4) --
// una tercera clase que usa ese nombre como tipo de atributo confirma que
// ninguna sobrevivio: debe fallar con ERR_TIPO_NO_DECLARADO, no resolver
// contra ninguna de las dos declaraciones descartadas. Se esperan los dos
// codigos en la misma corrida: ERR_CLASE_DUPLICADA (linea 9) y
// ERR_TIPO_NO_DECLARADO (linea 12). Verificado contra la salida real del
// compilador (no solo deducido).
class Figura{}
class Figura{}

class Caja{
    Figura contenido;
}
