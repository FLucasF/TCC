package com.loja.checkout.entrega;

import com.loja.checkout.ErroPedido;
import com.loja.checkout.Pedido;

import java.math.BigDecimal;

public class Motoboy implements ModalidadeEntrega {
    private static final BigDecimal CUSTO = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO = new BigDecimal("5");

    @Override
    public void validar(Pedido pedido) {
        if (pedido.pesoKg().compareTo(PESO_MAXIMO) > 0) {
            throw new ErroPedido("MODALIDADE_INDISPONIVEL");
        }
    }

    @Override
    public BigDecimal frete(Pedido pedido) {
        return CUSTO;
    }

    @Override
    public int prazoDias() { return 0; }
}
