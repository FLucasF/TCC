package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.ContextoPedido;
import com.loja.checkout.dto.ItemDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Leve3Pague2 implements Cupom {

    private static final int TAMANHO_DO_GRUPO = 3;

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal calcularDesconto(ContextoPedido contexto, BigDecimal frete) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemDTO item : contexto.itens()) {
            int unidadesGratis = item.quantidade() / TAMANHO_DO_GRUPO;
            desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
        }
        return desconto;
    }
}
