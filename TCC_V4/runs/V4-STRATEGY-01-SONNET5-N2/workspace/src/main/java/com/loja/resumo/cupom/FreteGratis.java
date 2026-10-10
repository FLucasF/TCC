package com.loja.resumo.cupom;

import com.loja.resumo.model.ItemPedido;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean aplicavel(List<ItemPedido> itens, BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        return frete;
    }
}
