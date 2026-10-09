package com.loja.checkout.service.cupom;

import com.loja.checkout.dto.ItemCarrinho;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;

@Component
public class CupomFreteGratis implements EstrategiaCupom {

    @Override
    public String getCodigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean aplicavel(List<ItemCarrinho> itens, BigDecimal subtotal, BigDecimal frete) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(List<ItemCarrinho> itens, BigDecimal subtotal, BigDecimal frete) {
        // desconto = valor do frete
        return frete;
    }
}
