package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.comum.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Tarifa de R$ 3,49 somada ao total, sempre a vista. Nao e aceito quando o
 * total do pedido passa de R$ 1.000,00.
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
    public boolean atende(BigDecimal totalPedido) {
        return totalPedido.compareTo(TOTAL_MAXIMO) <= 0;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = Dinheiro.valor(totalPedido.add(TARIFA));
        return new ResultadoPagamento(totalFinal, totalFinal);
    }
}
