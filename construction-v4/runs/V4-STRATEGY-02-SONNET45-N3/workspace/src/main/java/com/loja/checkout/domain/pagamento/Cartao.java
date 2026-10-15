package com.loja.checkout.domain.pagamento;

import com.loja.checkout.util.Dinheiro;

import java.math.BigDecimal;

public class Cartao implements FormaPagamento {

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= 3) {
            return Dinheiro.de(0.00);
        }

        double taxa = 0.0199;
        double fatorPrice = taxa / (1 - Math.pow(1 + taxa, -parcelas));
        BigDecimal valorParcela = Dinheiro.arredondar(
            totalPedido.multiply(BigDecimal.valueOf(fatorPrice))
        );

        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        return Dinheiro.arredondar(totalFinal.subtract(totalPedido));
    }

    @Override
    public boolean validarParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public boolean aceitaTotal(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, BigDecimal totalFinal, int parcelas) {
        if (parcelas <= 1) {
            return totalFinal;
        }

        if (parcelas > 3) {
            double taxa = 0.0199;
            double fatorPrice = taxa / (1 - Math.pow(1 + taxa, -parcelas));
            return Dinheiro.arredondar(totalPedido.multiply(BigDecimal.valueOf(fatorPrice)));
        }

        return Dinheiro.arredondar(totalFinal.divide(BigDecimal.valueOf(parcelas), 2, java.math.RoundingMode.HALF_EVEN));
    }
}
