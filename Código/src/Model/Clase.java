package Model;

import java.util.*;

public class Clase implements Chequeable, EntidadDeclarada {
    private Token nombre;
    private Token herencia;
    private HashMap<String, Atributo> atributos;
    private HashMap<String, Metodo> metodos;
    private List<Constructor> constructores ;
    private boolean tieneConstructor;
    private List<Interfaz> interfaces;
    private boolean consolidado = false;
    private boolean enCiclo = false;
    private final TablaSimbolos tablaSimbolos;

    public Clase(Token nombre, TablaSimbolos tablaSimbolos) {
        atributos = new HashMap<String,Atributo>();
        metodos = new HashMap<String,Metodo>();
        constructores = new LinkedList<Constructor>();
        interfaces = new ArrayList<Interfaz>();
        this.nombre = nombre;
        this.tablaSimbolos = tablaSimbolos;
    }

    public String getName()
    {
        return nombre.getLexema();
    }

    public Token getNombre()
    {
        return nombre;
    }

    @Override
    public int getLinea() {
        return nombre.getLinea();
    }

    public void agregarAtributo(Atributo atributo)
    {
        String firma = atributo.getFirma();
        if(atributos.containsKey(firma)){
            tablaSimbolos.agregarError(new ErrorSemantico(
                    "ERR_ATRIBUTO_DUPLICADO", atributo.getNombre().getLinea(), atributo.getNombre().getLexema()));
        }
        else
        {
            atributos.put(firma,atributo);
        }
    }

    /** Devuelve false si ya había un método con la misma firma (nombre +
     *  tipos de parámetros, ver Metodo.getFirma()) — key por firma, no por
     *  nombre, para que las sobrecargas legítimas convivan en el mapa. */
    public void agregarMetodo(Metodo metodo)
    {
        String firma = metodo.getFirma();
        if (metodos.containsKey(firma))
        {
            tablaSimbolos.agregarError(new ErrorSemantico(
                    "ERR_METODO_DUPLICADO", metodo.getNombre().getLinea(), metodo.getNombre().getLexema()));
            return;
        }
        metodos.put(firma, metodo);
    }

    public void agregarConstructor(Constructor constructor)
    {
        String firma = constructor.getFirma();
        for (Constructor existente : constructores) {
            if (existente.getFirma().equals(firma)) {
                tablaSimbolos.agregarError(new ErrorSemantico(
                        "ERR_CONSTRUCTOR_DUPLICADO", constructor.getNombre().getLinea(), constructor.getNombre().getLexema()));
                return;
            }
        }
        constructores.add(constructor);
        tieneConstructor = true;
    }

    public void setHerencia(Token herencia)
    {
        this.herencia = herencia;
    }

    @Override
    public void estaBienDeclarado(TablaSimbolos tabla)
    {
        for(Metodo metodo: metodos.values())
        {
           metodo.estaBienDeclarado(tabla);
        }
        for(Atributo atributo: atributos.values())
        {
           atributo.estaBienDeclarado(tabla);
        }
        for(Constructor constructor: constructores){
           constructor.estaBienDeclarado(tabla);
        }
    }

    // Mezcla en esta clase los métodos/atributos heredados de 'padre' (ya
    // resuelto y validado por consolidarHerencia — acá no se vuelve a chequear
    // ciclo ni existencia). No sobreescribe lo que esta clase ya declaró.
    private void consolidarConPadre(Clase padre) {
        for (Metodo metodo : padre.metodos.values())
        {
            if (!metodos.containsKey(metodo.getFirma()))
            {
                metodos.put(metodo.getFirma(), metodo);
            }
        }
        for (Atributo atributo : padre.atributos.values())
        {
            if (atributos.containsKey(atributo.getFirma()))
            {
                //lanzar error, esto signfica que tengo de nuevo el atributo
            }
            else
            {
                atributos.put(atributo.getFirma(), atributo);
            }
        }
        //TODO: validar que esta clase implemente los métodos de 'interfaces'
        // (pendiente: el parser todavía no puebla esta lista — Pasada 2).
    }


    public void consolidar() {
        consolidarHerencia(new LinkedHashSet<Clase>());
    }
    /*
        consolidarHerencia
        Camina la cadena de 'extends' de esta clase resolviendo el padre contra
        la tabla de símbolos (inyectada por constructor). 'camino' es la pila
        de recursión actual (las clases entre la raíz de este recorrido y
        'this'): si el padre ya está ahí, hay un ciclo.

        - Si el padre no existe en la tabla: no es un ciclo, es tipo no
          declarado.
        - Si el padre ya está en 'camino': es un ciclo. Se reporta una sola
          vez, sobre esta clase (la última visitada antes de repetir), y se
          marca 'enCiclo' en todo el tramo del camino que efectivamente cierra
          el ciclo — así, cuando TablaSimbolos.consolidar() le llegue el turno
          a cualquier otra clase de ese mismo ciclo (el recorrido llama a esto
          una vez por cada clase de la tabla, no una vez por ciclo), el guard
          de abajo corta antes de volver a caminar y volver a reportar.
        - 'consolidado'/'enCiclo' son memoization: si esta clase ya fue
          resuelta (bien o mal) en una pasada anterior, no se repite trabajo
          ni error.
     */
    public void consolidarHerencia(LinkedHashSet<Clase> camino)
    {
        if (consolidado || enCiclo)
        {
            return;
        }
        camino.add(this);
        if (herencia != null) {
            EntidadDeclarada padreEntidad = tablaSimbolos.buscarClase(herencia.getLexema());
            if (padreEntidad == null) {
                tablaSimbolos.agregarError(new ErrorSemantico(
                        "ERR_TIPO_NO_DECLARADO", nombre.getLinea(), herencia.getLexema()));
                camino.remove(this);
                return;
            }
            Clase padre = (Clase) padreEntidad;
            if (camino.contains(padre)) {
                tablaSimbolos.agregarError(new ErrorSemantico(
                        "ERR_HERENCIA_CICLICA", nombre.getLinea(), nombre.getLexema()));
                marcarCicloDesde(padre, camino);
                camino.remove(this);
                return;
            }
            padre.consolidarHerencia(camino);
            if (padre.enCiclo) {
                enCiclo = true;
                camino.remove(this);
                return;
            }
            consolidarConPadre(padre);
        }
        consolidado = true;
        camino.remove(this);
    }

    // Marca enCiclo=true solo en el tramo del camino que cierra el ciclo
    // (desde el nodo repetido 'padre' hasta el final, en orden de inserción) —
    // las clases anteriores en 'camino' (si las hay) dependen de este ciclo
    // pero no forman parte de él, y se marcan por su cuenta al propagarse
    // 'enCiclo' hacia atrás en la recursión.
    private void marcarCicloDesde(Clase padre, LinkedHashSet<Clase> camino)
    {
        boolean dentroDelCiclo = false;
        for (Clase c : camino)
        {
            if (c == padre)
            {
                dentroDelCiclo = true;
            }
            if (dentroDelCiclo)
             {
                c.enCiclo = true;
            }
        }
    }
}
