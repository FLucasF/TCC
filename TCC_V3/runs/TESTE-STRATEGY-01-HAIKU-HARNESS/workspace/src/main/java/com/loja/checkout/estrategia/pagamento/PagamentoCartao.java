package com.loja.checkout.estrategia.pagamento;

import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;

public class PagamentoCartao implements EstrategiaPagamento {
    @Override
    public BigDecimal calcularAjuste(BigDecimal total, int parcelas) {
        if (parcelas <= 3) {
            return BigDecimal.ZERO;
        }
        BigDecimal taxaMensal = new BigDecimal("0.0199");
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxaMensal);
        BigDecimal potencia = umMaisTaxa.pow(parcelas);
        BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(potencia, 10, java.math.RoundingMode.HALF_EVEN));
        BigDecimal numerador = total.multiply(taxaMensal);
        BigDecimal parcela = numerador.divide(denominador, 2, java.math.RoundingMode.HALF_EVEN);
        BigDecimal totalComJuros = parcela.multiply(new BigDecimal(parcelas));
        return totalComJuros.subtract(total);
    }

    @Override
    public void validar(int parcelas, BigDecimal totalPedido) throws IllegalArgumentException {
        if (parcelas < 1 || parcelas > 12) {
            throw new IllegalArgumentException("PARCELAMENTO_INVALIDO");
        }
    }
}
