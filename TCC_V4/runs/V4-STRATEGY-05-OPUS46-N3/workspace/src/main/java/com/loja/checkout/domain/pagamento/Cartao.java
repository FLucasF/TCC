package com.loja.checkout.domain.pagamento;

import com.loja.checkout.domain.Moeda;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class Cartao implements FormaPagamento {

    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final int MAX_SEM_JUROS = 3;
    private static final int MAX_PARCELAS = 12;

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas >= 1 && parcelas <= MAX_PARCELAS;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= MAX_SEM_JUROS) {
            BigDecimal valorParcela = Moeda.arredondar(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN));
            return new ResultadoPagamento(totalPedido, valorParcela);
        }

        double taxa = TAXA_MENSAL.doubleValue();
        double fator = Math.pow(1 + taxa, -parcelas);
        BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.valueOf(fator));
        BigDecimal numerador = totalPedido.multiply(TAXA_MENSAL);
        BigDecimal valorParcela = Moeda.arredondar(
                numerador.divide(denominador, 10, RoundingMode.HALF_EVEN));
        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        return new ResultadoPagamento(totalFinal, valorParcela);
    }
}
