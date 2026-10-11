package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.erro.CodigoErro;
import com.loja.checkout.erro.PedidoInvalidoException;
import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Pix implements FormaPagamento {

    private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("0.05");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public void validarParcelas(int parcelas) {
        if (parcelas != 1) {
            throw new PedidoInvalidoException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
    }

    @Override
    public AjustePagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Dinheiro.arredondar(totalPedido.multiply(PERCENTUAL_DESCONTO));
        BigDecimal totalFinal = totalPedido.subtract(desconto);
        return new AjustePagamento(desconto.negate(), totalFinal, totalFinal);
    }
}
