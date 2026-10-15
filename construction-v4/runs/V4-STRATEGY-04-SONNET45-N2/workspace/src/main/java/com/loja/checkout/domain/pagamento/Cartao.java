package com.loja.checkout.domain.pagamento;

import com.loja.checkout.domain.FormaPagamento;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import static com.loja.checkout.util.Arredondamento.arredondar;

public class Cartao implements FormaPagamento {

    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");

    @Override
    public BigDecimal calcularValorFinal(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= 3) {
            return totalPedido;
        }

        MathContext mc = new MathContext(10, RoundingMode.HALF_EVEN);
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS);
        BigDecimal umMaisTaxaPotencia = umMaisTaxa.pow(parcelas, mc);
        BigDecimal divisor = BigDecimal.ONE.subtract(
            BigDecimal.ONE.divide(umMaisTaxaPotencia, 10, RoundingMode.HALF_EVEN)
        );

        BigDecimal valorParcela = arredondar(
            totalPedido.multiply(TAXA_JUROS).divide(divisor, 10, RoundingMode.HALF_EVEN)
        );

        return valorParcela.multiply(BigDecimal.valueOf(parcelas));
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public boolean aceita(BigDecimal totalPedido) {
        return true;
    }
}
