package com.loja.checkout.cupom;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import com.loja.checkout.model.Pedido;
import com.loja.checkout.model.Resumo;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component("MENOS50")
public class Menos50 implements Cupom {
    private static final BigDecimal DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal VALOR_MINIMO = new BigDecimal("300.00");

    @Override
    public BigDecimal calcularDesconto(Pedido pedido, Resumo resumo) {
        return DESCONTO;
    }

    @Override
    public void validarAplicabilidade(Pedido pedido) {
        if (pedido.subtotalProdutos().compareTo(VALOR_MINIMO) < 0) {
            throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
    }
}
