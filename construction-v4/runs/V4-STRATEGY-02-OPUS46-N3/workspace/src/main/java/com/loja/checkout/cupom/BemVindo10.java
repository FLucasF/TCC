package com.loja.checkout.cupom;

import com.loja.checkout.Moeda;
import com.loja.checkout.ResumoRequest.ItemRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class BemVindo10 implements Cupom {

    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public boolean aplicavel(List<ItemRequest> itens, BigDecimal subtotal) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(List<ItemRequest> itens, BigDecimal subtotal, BigDecimal frete) {
        return Moeda.arredondar(subtotal.multiply(PERCENTUAL));
    }
}
