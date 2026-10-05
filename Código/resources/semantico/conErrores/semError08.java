///[ErrorSem:ERR_CLASE_DUPLICADA|6]
// Clase e interfaz comparten el mismo espacio de nombres (Pasada 1 del EDT:
// tabla.insertarClase(...) para las dos) -- este caso debe reportarse igual
// que dos clases con el mismo nombre, no como un error distinto.
class Figura{}
interface Figura{}
