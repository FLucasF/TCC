package com.loja.checkout.cupom;

import com.loja.checkout.CheckoutException;
import com.loja.checkout.ErroCode;
import com.loja.checkout.dto.ItemRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CupomMenos50 implements Cupom {

    private static final BigDecimal MINIMO = new BigDecimal("300.00");
    private static final BigDecimal DESCONTO = new BigDecimal("50.00");

    @Override
    public String getCodigo() {
        return "MENOS50";
    }

    @Override
    public void verificarAplicabilidade(BigDecimal subtotalProdutos, BigDecimal frete) {
        if (subtotalProdutos.compareTo(MINIMO) < 0) {
            throw new CheckoutException(ErroCode.CUPOM_NAO_APLICAVEL);
        }
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens) {
        return DESCONTO;
    }
}
