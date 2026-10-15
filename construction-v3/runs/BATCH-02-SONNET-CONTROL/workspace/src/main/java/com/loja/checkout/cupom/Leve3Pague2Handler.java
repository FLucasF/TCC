package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Leve3Pague2Handler implements CupomHandler {

    private static final int TAMANHO_LOTE = 3;

    @Override
    public String getCodigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(DadosPedido pedido) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(DadosPedido pedido) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemRequest item : pedido.itens()) {
            int unidadesGratis = item.quantidade() / TAMANHO_LOTE;
            if (unidadesGratis > 0) {
                desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
            }
        }
        return Dinheiro.arredondar(desconto);
    }
}
