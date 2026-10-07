package Model.semantico;

import Model.lexico.Token;

import java.util.HashMap;
import java.util.LinkedHashSet;

public class Interfaz implements Chequeable, EntidadDeclarada {
    private Token nombre;
    private Parametro genericoOpcional;
    private Token extendida;
    private HashMap<Token,Metodo> metodos = new HashMap<>();
    private boolean consolidado = false;
    private boolean enCiclo = false;
    private final TablaSimbolos tablaSimbolos;

    public Interfaz(Token nombre, TablaSimbolos tablaSimbolos) {
        this.nombre = nombre;
        this.tablaSimbolos = tablaSimbolos;
    }

    @Override
    public String getName() {
        return nombre.getLexema();
    }

    public Token getNombre() {
        return nombre;
    }

    @Override
    public int getLinea() {
        return nombre.getLinea();
    }

    public void setExtendida(Token extendida) {
        this.extendida = extendida;
    }

    @Override
    public void estaBienDeclarado(TablaSimbolos tabla) {
        // TODO: chequeo de corrección diferido — ver "TODO — Chequeo de
        // corrección" en analizador_semantico.md. metodos todavía no se puebla
        // (metodoInterfaz() sigue siendo puramente sintáctico, Pasada 2).
        for (Metodo metodo : metodos.values()) {
            metodo.estaBienDeclarado(tabla);
        }
    }

    public Iterable<Metodo> getMetodos(){
        return metodos.values();
    }

    // Único punto de entrada público, igual que Clase.consolidar(): construye
    // su propio camino de recursión para no depender de quien lo llama.
    public void consolidar() {
        consolidar(new LinkedHashSet<Interfaz>());
    }

    // Mismo algoritmo que Clase.consolidarHerencia (ver su comentario), pero
    // sobre la cadena "extends" de Interfaz, que es un campo/objeto distinto.
    private void consolidar(LinkedHashSet<Interfaz> camino) {
        if (consolidado || enCiclo) {
            return;
        }
        camino.add(this);
        if (extendida != null) {
            EntidadDeclarada padreEntidad = tablaSimbolos.buscarClase(extendida.getLexema());
            if (padreEntidad == null) {
                tablaSimbolos.agregarError(new ErrorSemantico(
                        "ERR_TIPO_NO_DECLARADO", nombre.getLinea(), extendida.getLexema()));
                camino.remove(this);
                return;
            }
            if (!(padreEntidad instanceof Interfaz)) {
                // 'extends' de interfaz apuntando a algo que no es interfaz:
                // fuera de alcance por ahora (pregunta abierta, ver
                // propuesta_casos_test_semantico.md 3.6).
                consolidado = true;
                camino.remove(this);
                return;
            }
            Interfaz padre = (Interfaz) padreEntidad;
            if (camino.contains(padre)) {
                tablaSimbolos.agregarError(new ErrorSemantico(
                        "ERR_HERENCIA_CICLICA", nombre.getLinea(), nombre.getLexema()));
                marcarCicloDesde(padre, camino);
                camino.remove(this);
                return;
            }
            padre.consolidar(camino);
            if (padre.enCiclo) {
                enCiclo = true;
                camino.remove(this);
                return;
            }
        }
        consolidado = true;
        camino.remove(this);
    }

    private void marcarCicloDesde(Interfaz padre, LinkedHashSet<Interfaz> camino) {
        boolean dentroDelCiclo = false;
        for (Interfaz i : camino) {
            if (i == padre) {
                dentroDelCiclo = true;
            }
            if (dentroDelCiclo) {
                i.enCiclo = true;
            }
        }
    }
}
