package com.loja.checkout.cupom;

import java.math.BigDecimal;
import java.util.List;

import com.loja.checkout.dto.ItemRequest;
import org.springframework.stereotype.Component;

@Component
public class FreteGratis implements Cupom {

    @Override
    public String getCodigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean isAplicavel(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete) {
        return frete;
    }
}
