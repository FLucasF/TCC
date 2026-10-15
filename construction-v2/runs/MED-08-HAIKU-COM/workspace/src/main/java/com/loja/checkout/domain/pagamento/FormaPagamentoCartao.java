package com.loja.checkout.domain.pagamento;

import com.loja.checkout.utils.Arredondamento;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class FormaPagamentoCartao implements FormaPagamento {
    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final int PARCELAS_MAX_SEM_JUROS = 3;
    private static final int PARCELAS_MIN = 1;
    private static final int PARCELAS_MAX = 12;

    @Override
    public BigDecimal calcularAjuste(BigDecimal total, int parcelas) {
        if (parcelas <= PARCELAS_MAX_SEM_JUROS) {
            return BigDecimal.ZERO;
        }

        BigDecimal valorParcela = calcularParcelaComJuros(total, parcelas);
        BigDecimal totalComJuros = valorParcela.multiply(new BigDecimal(parcelas));
        return totalComJuros.subtract(total);
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalFinal, int parcelas) {
        BigDecimal parcela = totalFinal.divide(new BigDecimal(parcelas), 10, RoundingMode.HALF_EVEN);
        return Arredondamento.arredondarParaCentavos(parcela);
    }

    private BigDecimal calcularParcelaComJuros(BigDecimal total, int parcelas) {
        BigDecimal numerador = total.multiply(TAXA_MENSAL);

        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_MENSAL);
        BigDecimal potencia = umMaisTaxa.pow(parcelas);
        BigDecimal umDividoPotencia = BigDecimal.ONE.divide(potencia, 10, RoundingMode.HALF_EVEN);

        BigDecimal denominador = BigDecimal.ONE.subtract(umDividoPotencia);

        BigDecimal parcela = numerador.divide(denominador, 10, RoundingMode.HALF_EVEN);
        return Arredondamento.arredondarParaCentavos(parcela);
    }

    @Override
    public boolean podeAplicar(BigDecimal total, int parcelas) {
        return parcelas >= PARCELAS_MIN && parcelas <= PARCELAS_MAX;
    }

    @Override
    public String obterCodigo() {
        return "CARTAO";
    }
}
