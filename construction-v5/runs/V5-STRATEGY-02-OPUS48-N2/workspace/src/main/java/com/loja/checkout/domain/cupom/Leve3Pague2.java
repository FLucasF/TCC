package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.Dinheiro;
import com.loja.checkout.domain.ItemPedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** A cada 3 unidades de um mesmo item, uma sai de graca. */
@Component
public class Leve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public void validarAplicabilidade(DadosCupom dados) {
        // Sem condicao.
    }

    @Override
    public BigDecimal desconto(DadosCupom dados) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemPedido item : dados.itens()) {
            BigDecimal gratis = BigDecimal.valueOf(item.unidadesGratis3por2());
            total = total.add(item.precoUnitario().multiply(gratis));
        }
        return Dinheiro.centavos(total);
    }
}
