package com.loja.checkout.pagamento;

import com.loja.checkout.Arredondamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Cartao implements FormaPagamento {

    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelamentoValido(int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido, int parcelas) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= 3) {
            BigDecimal valorParcela = Arredondamento.centavos(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), 10, java.math.RoundingMode.HALF_EVEN)
            );
            return new ResultadoPagamento(totalPedido, valorParcela, parcelas);
        }

        double taxa = TAXA_MENSAL.doubleValue();
        double total = totalPedido.doubleValue();
        double fator = taxa / (1 - Math.pow(1 + taxa, -parcelas));
        double parcelaCalc = total * fator;

        BigDecimal valorParcela = Arredondamento.centavos(BigDecimal.valueOf(parcelaCalc));
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        return new ResultadoPagamento(totalFinal, valorParcela, parcelas);
    }
}
