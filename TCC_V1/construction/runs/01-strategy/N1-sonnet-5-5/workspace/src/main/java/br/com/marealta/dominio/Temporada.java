package br.com.marealta.dominio;

import java.time.LocalDate;
import java.time.Month;

public final class Temporada {

    private Temporada() {
    }

    /** Alta temporada: de 15/12 até o fim de fevereiro, e julho inteiro. */
    public static boolean alta(LocalDate noite) {
        Month mes = noite.getMonth();
        return mes == Month.JANUARY
                || mes == Month.FEBRUARY
                || mes == Month.JULY
                || (mes == Month.DECEMBER && noite.getDayOfMonth() >= 15);
    }
}
