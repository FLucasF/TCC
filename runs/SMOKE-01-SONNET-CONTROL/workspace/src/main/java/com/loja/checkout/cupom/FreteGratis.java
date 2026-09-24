package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component("FRETEGRATIS")
public class FreteGratis implements Cupom {

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens) {
        return frete;
    }
}
