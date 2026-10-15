package com.loja.checkout.payment;

import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Cartao implements PaymentMethod {

    private static final int PARCELAS_SEM_JUROS = 3;
    private static final int PARCELAS_MAXIMAS = 12;
    private static final double TAXA_JUROS_MENSAL = 0.0199;

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas >= 1 && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = Dinheiro.arredondar(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), 10, java.math.RoundingMode.HALF_EVEN));
            return new ResultadoPagamento(totalPedido, BigDecimal.ZERO.setScale(2), valorParcela);
        }

        double total = totalPedido.doubleValue();
        double fatorDesconto = 1 - Math.pow(1 + TAXA_JUROS_MENSAL, -parcelas);
        double parcelaCalculada = total * TAXA_JUROS_MENSAL / fatorDesconto;
        BigDecimal valorParcela = Dinheiro.arredondar(parcelaCalculada);
        BigDecimal totalFinal = Dinheiro.arredondar(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        return new ResultadoPagamento(totalFinal, ajuste, valorParcela);
    }
}
