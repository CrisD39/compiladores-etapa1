///[SinErrores]
// Clase generica con un unico parametro de tipo (<GenericidadOpcional>),
// usado como tipo de atributo, parametro y variable local -- idGen entra
// por <TipoBase> igual que idClase.
class Caja<T>{

    T contenido;

    T obtener(){
        return contenido;
    }

    void guardar(T valor){
        T copia;
        copia = valor;
        contenido = copia;
    }

}
