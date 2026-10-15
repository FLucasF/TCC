package com.loja.checkout.cupom;

import com.loja.checkout.model.ItemCarrinho;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class FreteGratis implements Cupom {
    @Override
    public String codigo() { return "FRETEGRATIS"; }

    @Override
    public void validarAplicabilidade(BigDecimal subtotalProdutos) {}

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemCarrinho> itens, BigDecimal frete) {
        return frete;
    }
}
