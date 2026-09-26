package com.loja.checkout.domain.pagamento;

import com.loja.checkout.domain.pedido.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Boleto: tarifa de R$ 3,49 do banco somada ao total, sempre a vista e
 * indisponivel acima de R$ 1.000,00 de total do pedido.
 */
@Component
public class PagamentoBoleto implements FormaPagamento {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000.00");

    @Override
    public String codigo() {
        return "BOLETO";
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean atende(BigDecimal totalSemImposto) {
        return totalSemImposto.compareTo(TOTAL_MAXIMO) <= 0;
    }

    @Override
    public ResultadoPagamento aplicar(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = Dinheiro.valor(totalPedido.add(TARIFA));
        return new ResultadoPagamento(totalFinal, totalFinal);
    }
}
