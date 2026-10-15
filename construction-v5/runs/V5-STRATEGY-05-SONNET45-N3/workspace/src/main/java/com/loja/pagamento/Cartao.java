package com.loja.pagamento;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Cartao implements FormaPagamento {

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= 3) {
            return BigDecimal.ZERO.setScale(2);
        }

        BigDecimal taxa = new BigDecimal("0.0199");
        BigDecimal fator = BigDecimal.ONE.add(taxa).pow(parcelas);
        BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, 10, RoundingMode.HALF_EVEN));

        BigDecimal valorParcela = totalPedido
            .multiply(taxa)
            .divide(denominador, 2, RoundingMode.HALF_EVEN);

        BigDecimal totalComJuros = valorParcela.multiply(BigDecimal.valueOf(parcelas));

        return totalComJuros.subtract(totalPedido).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean validarParcelamento(int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public boolean estaDisponivel(BigDecimal totalPedido, int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }
}
