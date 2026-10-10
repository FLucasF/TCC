package com.loja.checkout.service.cupom;

import com.loja.checkout.dto.ItemPedidoRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class FreteGratisCupom implements CalculadoraCupom {

    @Override
    public String getCodigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean aplicavel(List<ItemPedidoRequest> itens, BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(List<ItemPedidoRequest> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        return frete;
    }
}
