package Controller;

import Model.ErrorSemantico;
import Model.ErrorSintactico;

import java.util.List;

// Agrupa las dos listas de error que produce una corrida semántica completa
// (el sintáctico sigue acumulando los suyos igual que antes; el semántico es
// nuevo, ver TablaSimbolos.getErrores()).
public final class ResultadoAnalisisSemantico {
    private final List<ErrorSintactico> erroresSintacticos;
    private final List<ErrorSemantico> erroresSemanticos;

    public ResultadoAnalisisSemantico(
            List<ErrorSintactico> erroresSintacticos,
            List<ErrorSemantico> erroresSemanticos) {
        this.erroresSintacticos = erroresSintacticos;
        this.erroresSemanticos = erroresSemanticos;
    }

    public List<ErrorSintactico> getErroresSintacticos() {
        return erroresSintacticos;
    }

    public List<ErrorSemantico> getErroresSemanticos() {
        return erroresSemanticos;
    }
}
