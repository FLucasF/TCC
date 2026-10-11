package com.loja.checkout.service.entrega;

import com.loja.checkout.domain.Pedido;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class Motoboy implements Modalidade {
    private static final BigDecimal TAXA_FIXA = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO = new BigDecimal("5.00");

    @Override
    public String getCodigo() {
        return "MOTOBOY";
    }

    @Override
    public boolean aceita(Pedido pedido) {
        BigDecimal pesoTotal = pedido.itens().stream()
            .map(item -> item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return pesoTotal.compareTo(PESO_MAXIMO) <= 0;
    }

    @Override
    public BigDecimal calcularFrete(Pedido pedido) {
        return TAXA_FIXA.setScale(2);
    }

    @Override
    public int getPrazoDias() {
        return 0;
    }
}
