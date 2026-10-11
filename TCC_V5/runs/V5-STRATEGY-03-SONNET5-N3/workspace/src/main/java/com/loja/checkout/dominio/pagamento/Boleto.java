package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.erro.CodigoErro;
import com.loja.checkout.erro.PedidoInvalidoException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Boleto implements FormaPagamento {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000.00");

    @Override
    public String codigo() {
        return "BOLETO";
    }

    @Override
    public void validarParcelas(int parcelas) {
        if (parcelas != 1) {
            throw new PedidoInvalidoException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
    }

    @Override
    public void validarDisponibilidade(BigDecimal totalPedido) {
        if (totalPedido.compareTo(TOTAL_MAXIMO) > 0) {
            throw new PedidoInvalidoException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
    }

    @Override
    public AjustePagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = totalPedido.add(TARIFA);
        return new AjustePagamento(TARIFA, totalFinal, totalFinal);
    }
}
