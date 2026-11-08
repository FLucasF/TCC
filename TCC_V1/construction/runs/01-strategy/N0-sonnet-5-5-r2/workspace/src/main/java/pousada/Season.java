package pousada;

import java.time.LocalDate;

public final class Season {
    private Season() {}

    /** Alta temporada: 15/12 até o fim de fevereiro, e julho inteiro. */
    public static boolean isHigh(LocalDate night) {
        int m = night.getMonthValue();
        return m == 1 || m == 2 || m == 7 || (m == 12 && night.getDayOfMonth() >= 15);
    }
}
