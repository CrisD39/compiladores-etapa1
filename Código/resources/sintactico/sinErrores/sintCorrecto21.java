///[SinErrores]
// Interfaz basica: metodos sin cuerpo (terminan en ";", implicitamente
// publicos), sin extends. Cubre <Interfaz>/<MetodoInterfaz>, sin tocar
// todavia <ExtensionOpcional> ni genericidad.
interface Figura{

    int area();
    void dibujar();
    boolean esValida(int x, char c);

}
