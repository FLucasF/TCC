package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.exception.CheckoutException;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CupomMenos50 implements CalculoCupom {

    private static final BigDecimal MINIMO = new BigDecimal("300.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public void validar(List<ItemRequest> itens, BigDecimal subtotal) {
        if (subtotal.compareTo(MINIMO) < 0) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
    }

    @Override
    public BigDecimal calcularDesconto(List<ItemRequest> itens, BigDecimal subtotal, BigDecimal frete) {
        return new BigDecimal("50.00");
    }
}
