package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Cartao implements FormaPagamento {

    private static final int PARCELAS_SEM_JUROS = 3;
    private static final double TAXA_MENSAL = 0.0199;

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido, int parcelas) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = Dinheiro.arredondar(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), 10, java.math.RoundingMode.HALF_EVEN));
            return new ResultadoPagamento(BigDecimal.ZERO.setScale(2), totalPedido.setScale(2), valorParcela);
        }

        double fatorPrice = TAXA_MENSAL / (1 - Math.pow(1 + TAXA_MENSAL, -parcelas));
        BigDecimal valorParcela = Dinheiro.arredondar(totalPedido.doubleValue() * fatorPrice);
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        BigDecimal ajuste = Dinheiro.arredondar(totalFinal.subtract(totalPedido));
        return new ResultadoPagamento(ajuste, totalFinal, valorParcela);
    }
}
