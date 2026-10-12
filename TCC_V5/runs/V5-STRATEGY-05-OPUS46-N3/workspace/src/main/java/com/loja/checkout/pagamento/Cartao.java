package com.loja.checkout.pagamento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

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
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= 3) {
            BigDecimal valorParcela = totalPedido
                    .divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN);
            return new ResultadoPagamento(totalPedido, valorParcela);
        }
        BigDecimal base = BigDecimal.ONE.add(TAXA_MENSAL);
        BigDecimal potencia = base.pow(parcelas);
        BigDecimal inversoPotencia = BigDecimal.ONE
                .divide(potencia, 20, RoundingMode.HALF_EVEN);
        BigDecimal denominador = BigDecimal.ONE.subtract(inversoPotencia);
        BigDecimal valorParcela = totalPedido.multiply(TAXA_MENSAL)
                .divide(denominador, 2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = valorParcela.multiply(new BigDecimal(parcelas));
        return new ResultadoPagamento(totalFinal, valorParcela);
    }
}
