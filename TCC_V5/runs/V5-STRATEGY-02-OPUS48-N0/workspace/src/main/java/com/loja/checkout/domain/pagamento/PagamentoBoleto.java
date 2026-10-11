package com.loja.checkout.domain.pagamento;

import com.loja.checkout.domain.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Boleto: sempre a vista, tarifa de R$ 3,49 somada ao total. Nao aceito quando
 * o total do pedido passa de R$ 1.000,00.
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
    public boolean parcelamentoValido(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return totalPedido.compareTo(TOTAL_MAXIMO) <= 0;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = Dinheiro.centavos(totalPedido.add(TARIFA));
        return new ResultadoPagamento(1, totalFinal, totalFinal, TARIFA);
    }
}
