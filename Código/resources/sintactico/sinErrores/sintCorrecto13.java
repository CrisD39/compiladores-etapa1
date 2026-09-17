///[SinErrores]
// "for-each" (REQ-AS-009) con distintos tipos de variable de iteracion:
// primitivo, idGen, idClase simple y generico; cuerpo con y sin llaves;
// anidado con otro for-each.

class ForEachOk{

    static void metodo()
    {
        for (int x : arreglo)
        {
            var y = x;
        }

        for (T t : coleccion)
        {
        }

        for (Item it : lista)
            procesar(it);

        for (Caja<Item> caja : cajas)
        {
        }

        for (Item it : lista)
            for (int x : arreglo)
            {
                var y = x;
            }
    }

}
