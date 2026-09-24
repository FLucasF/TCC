package com.loja.checkout.pagamento;

import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component("BOLETO")
public class Boleto implements FormaPagamento {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_PEDIDO = new BigDecimal("1000.00");

    @Override
    public void validarParcelas(int parcelas) {
        if (parcelas != 1) {
            parcelamentoInvalido();
        }
    }

    @Override
    public void validarDisponibilidade(BigDecimal totalPedido) {
        if (totalPedido.compareTo(LIMITE_PEDIDO) > 0) {
            indisponivel();
        }
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.add(TARIFA));
        return new ResultadoPagamento(totalFinal, totalFinal);
    }
}
