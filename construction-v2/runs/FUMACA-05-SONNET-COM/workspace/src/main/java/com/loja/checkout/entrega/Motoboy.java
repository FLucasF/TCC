package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Motoboy implements OpcaoEntrega {

    private static final BigDecimal PRECO = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public boolean disponivelPara(Pedido pedido) {
        return pedido.pesoTotal().compareTo(PESO_MAXIMO_KG) <= 0;
    }

    @Override
    public BigDecimal calcularFrete(Pedido pedido) {
        return PRECO;
    }

    @Override
    public int prazoDias() {
        return 0;
    }
}
