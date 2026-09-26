package com.loja.checkout.pagamento;

import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PagamentoCartao implements FormaPagamento {

    private static final int PARCELAS_SEM_JUROS = 3;
    private static final int PARCELAS_MAXIMAS = 12;
    private static final double TAXA_JUROS_MENSAL = 0.0199;

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas >= 1 && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public boolean disponivelPara(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal ajuste = BigDecimal.ZERO.setScale(2);
            BigDecimal totalFinal = totalPedido;
            BigDecimal valorParcela = Dinheiro.arredondar(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN));
            return new ResultadoPagamento(ajuste, totalFinal, valorParcela);
        }

        double totalD = totalPedido.doubleValue();
        double fatorDesconto = Math.pow(1 + TAXA_JUROS_MENSAL, -parcelas);
        double parcelaD = totalD * TAXA_JUROS_MENSAL / (1 - fatorDesconto);
        BigDecimal valorParcela = Dinheiro.arredondar(BigDecimal.valueOf(parcelaD));
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        return new ResultadoPagamento(ajuste, totalFinal, valorParcela);
    }
}
