///[SinErrores]
// Inicializadores de atributo (REQ-AS-011): "= <ExpresionCompuesta>" opcional
// al declarar un atributo, con tipo primitivo, idClase y arreglo (con "new",
// no llaves - eso es REQ-AS-012). Convive con atributos sin inicializar,
// visibilidad, constructor y metodo en la misma clase.

class InicializadorOk{

    private int contador = 0;
    public String nombre = "vacio";
    Item item = new Item();
    int[] arr = new int[5];
    boolean activo;

    public InicializadorOk(int c)
    {
        contador = c;
    }

    void metodo()
    {
    }

}
