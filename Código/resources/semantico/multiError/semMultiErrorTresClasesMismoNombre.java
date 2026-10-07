// Tres (no solo dos) clases con el mismo nombre: confirma que el analisis
// no se detiene tras el primer duplicado y que ninguna de las tres termina
// sobreviviendo en la tabla, sin importar la paridad de la cantidad de
// repeticiones. Se esperan DOS ERR_CLASE_DUPLICADA (lineas 12 y 13 -- la
// segunda y la tercera declaracion, cada una contra lo que ya estaba
// invalidado) y, al final, ERR_TIPO_NO_DECLARADO sobre "Figura" en Caja
// (linea 16 -- ninguna de las tres sobrevive). Antes de arreglar
// TablaSimbolos.insertarClase (ver nombresInvalidados), la TERCERA
// declaracion "revivia" el nombre -- hoy queda invalidado para siempre en
// cuanto se detecta la primera repeticion. Verificado contra la salida real.
class Figura{}
class Figura{}
class Figura{}

class Caja{
    Figura contenido;
}
