package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class Menos50 implements Cupom {

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(List<ItemRequest> itens, BigDecimal subtotal) {
        return subtotal.compareTo(new BigDecimal("300.00")) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(List<ItemRequest> itens, BigDecimal subtotal, BigDecimal frete) {
        return new BigDecimal("50.00");
    }
}
