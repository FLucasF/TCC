package br.com.marealta.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Regras de dinheiro: centavos, arredondamento meio para o par e formato brasileiro. */
public final class Dinheiro {

    public static final BigDecimal ZERO = arredondar(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal valor(String texto) {
        return arredondar(new BigDecimal(texto));
    }

    /** Aplica um percentual (ex.: 12 para 12%) ao valor e arredonda para centavos. */
    public static BigDecimal percentual(BigDecimal valor, int percentual) {
        return arredondar(valor.multiply(BigDecimal.valueOf(percentual)).divide(BigDecimal.valueOf(100)));
    }

    public static String formatar(BigDecimal valor) {
        DecimalFormat formato = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(new Locale("pt", "BR")));
        formato.setRoundingMode(RoundingMode.HALF_EVEN);
        return "R$ " + formato.format(valor);
    }
}
