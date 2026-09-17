///[SinErrores]
// Visibilidad de miembros (REQ-AS-007): "public"/"private" opcional en
// atributos, metodos y constructores; el constructor ya no exige "public".
// Incluye un atributo de tipo clase generica para ejercitar
// trasIdClaseMiembro() con <TipoGenericoOpcional>.

class Persona{

    private int edad;
    public String nombre;
    Direccion casa;
    private Caja<Item> caja;

    public Persona(int edad)
    {
        this.edad = edad;
    }

    private Persona()
    {
    }

    public int getEdad()
    {
        return edad;
    }

    private void privado()
    {
    }

    void sinVisibilidad()
    {
    }

}
