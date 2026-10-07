///[SinErrores]
// Sobrecarga real dentro de una sola clase: mismo nombre, distintos
// parametros (int vs char) -- no debe ser ERR_METODO_DUPLICADO. Antes de
// este arreglo, crearMetodo() registraba el metodo en Clase.metodos con
// la firma calculada sobre el placeholder (params=[]) ANTES de parsear
// los parametros reales, asi que dos sobrecargas cualquiera colisionaban.
class Pato{

    void mover(int pasos){

    }

    void mover(char pasos){

    }

}
