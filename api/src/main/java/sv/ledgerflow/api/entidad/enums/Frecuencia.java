package sv.ledgerflow.api.entidad.enums;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public enum Frecuencia {
    DIARIA(ChronoUnit.DAYS),
    SEMANAL(ChronoUnit.WEEKS),
    MENSUAL(ChronoUnit.MONTHS),
    ANUAL(ChronoUnit.YEARS);

    private final ChronoUnit unidad;

    Frecuencia(ChronoUnit unidad) {
        this.unidad = unidad;
    }

    public LocalDate siguiente(LocalDate fecha) {
        return fecha.plus(1, unidad);
    }
}
