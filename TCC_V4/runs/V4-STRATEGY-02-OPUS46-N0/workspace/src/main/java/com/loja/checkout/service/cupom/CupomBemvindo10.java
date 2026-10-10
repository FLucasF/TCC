package com.loja.checkout.service.cupom;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.service.Moeda;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CupomBemvindo10 implements ProcessadorCupom {

    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public boolean aplicavel(BigDecimal subtotal, List<ItemRequest> itens) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete) {
        return Moeda.arredondar(subtotal.multiply(PERCENTUAL));
    }
}
