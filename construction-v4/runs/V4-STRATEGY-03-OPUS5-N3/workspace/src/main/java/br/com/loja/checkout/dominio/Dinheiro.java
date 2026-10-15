package br.com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Valor em dinheiro. Todo valor nasce arredondado para centavos no criterio
 * meio-para-o-par, que e o arredondamento usado em todas as etapas do resumo.
 */
public record Dinheiro(BigDecimal valor) implements Comparable<Dinheiro> {

    private static final int CENTAVOS = 2;
    private static final RoundingMode CRITERIO = RoundingMode.HALF_EVEN;

    public static final Dinheiro ZERO = de("0");

    public Dinheiro {
        valor = valor.setScale(CENTAVOS, CRITERIO);
    }

    public static Dinheiro de(String valor) {
        return new Dinheiro(new BigDecimal(valor));
    }

    public static Dinheiro de(BigDecimal valor) {
        return new Dinheiro(valor);
    }

    public Dinheiro mais(Dinheiro outro) {
        return new Dinheiro(valor.add(outro.valor));
    }

    public Dinheiro menos(Dinheiro outro) {
        return new Dinheiro(valor.subtract(outro.valor));
    }

    public Dinheiro vezes(BigDecimal fator) {
        return new Dinheiro(valor.multiply(fator));
    }

    public Dinheiro vezes(int fator) {
        return vezes(BigDecimal.valueOf(fator));
    }

    public Dinheiro divididoPor(int divisor) {
        return new Dinheiro(valor.divide(BigDecimal.valueOf(divisor), CENTAVOS, CRITERIO));
    }

    public boolean maiorQue(Dinheiro outro) {
        return compareTo(outro) > 0;
    }

    public boolean menorQue(Dinheiro outro) {
        return compareTo(outro) < 0;
    }

    @Override
    public int compareTo(Dinheiro outro) {
        return valor.compareTo(outro.valor);
    }
}
