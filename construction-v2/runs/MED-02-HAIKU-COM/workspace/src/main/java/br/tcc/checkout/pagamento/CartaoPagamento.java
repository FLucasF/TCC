package br.tcc.checkout.pagamento;

import java.math.BigDecimal;
import br.tcc.checkout.util.Arredondador;

public class CartaoPagamento implements FormaPagamento {
    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");

    @Override
    public BigDecimal calcularTotalFinal(BigDecimal total, Integer parcelas) {
        if (parcelas <= 3) {
            return Arredondador.arredondar(total);
        }

        BigDecimal valorParcela = calcularValorParcela(total, parcelas);
        return Arredondador.arredondar(valorParcela.multiply(new BigDecimal(parcelas)));
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal total, Integer parcelas) {
        if (parcelas <= 3) {
            return Arredondador.arredondar(total.divide(new BigDecimal(parcelas), 10, java.math.RoundingMode.HALF_EVEN));
        }

        BigDecimal um = new BigDecimal("1");
        BigDecimal umMaisTaxa = um.add(TAXA_JUROS);
        BigDecimal umMaisTaxaElevado = umMaisTaxa.pow(parcelas);
        BigDecimal denominador = um.subtract(um.divide(umMaisTaxaElevado, 10, java.math.RoundingMode.HALF_EVEN));
        BigDecimal parcela = total.multiply(TAXA_JUROS).divide(denominador, 10, java.math.RoundingMode.HALF_EVEN);

        return Arredondador.arredondar(parcela);
    }

    @Override
    public boolean parcelavalida(Integer parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }
}
