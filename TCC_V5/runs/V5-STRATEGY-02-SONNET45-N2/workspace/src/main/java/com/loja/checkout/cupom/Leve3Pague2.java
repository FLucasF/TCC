package com.loja.checkout.cupom;

import com.loja.checkout.model.Item;
import com.loja.checkout.model.Pedido;
import com.loja.checkout.model.Resumo;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component("LEVE3PAGUE2")
public class Leve3Pague2 implements Cupom {
    @Override
    public BigDecimal calcularDesconto(Pedido pedido, Resumo resumo) {
        return pedido.itens().stream()
            .map(this::calcularDescontoItem)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularDescontoItem(Item item) {
        int unidadesGratis = item.quantidade() / 3;
        return item.precoUnitario().multiply(new BigDecimal(unidadesGratis));
    }

    @Override
    public void validarAplicabilidade(Pedido pedido) {
    }
}
