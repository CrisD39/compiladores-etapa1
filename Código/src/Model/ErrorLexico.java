package Model;

// Datos necesarios para el reporte de REQ-MP-08: línea, razón, la línea fuente completa
// y la columna del carácter que produjo el error, para poder dibujar el "^" debajo.
public final class ErrorLexico {

    private final int linea;
    private final int columna;
    private final String lexema;
    private final String razon;
    private final String lineaFuente;

    public ErrorLexico(int linea, int columna, String lexema, String razon, String lineaFuente) {
        this.linea = linea;
        this.columna = columna;
        this.lexema = lexema;
        this.razon = razon;
        this.lineaFuente = lineaFuente;
    }

    public int getLinea() {
        return linea;
    }

    public int getColumna() {
        return columna;
    }

    public String getLexema() {
        return lexema;
    }

    public String getRazon() {
        return razon;
    }

    public String getLineaFuente() {
        return lineaFuente;
    }
}
