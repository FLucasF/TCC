package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.ItemCarrinho;
import com.loja.checkout.dominio.Moeda;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** A cada 3 unidades de um mesmo item do carrinho, uma sai de graca. */
@Component
public class CupomLeve3Pague2 implements Cupom {

    private static final int UNIDADES_POR_TRIO = 3;

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    /** Precisa de pelo menos um trio no carrinho; senao a promocao nao da desconto nenhum. */
    @Override
    public boolean aplicavel(DadosCupom dados) {
        return dados.itens().stream().anyMatch(item -> unidadesGratis(item) > 0);
    }

    @Override
    public BigDecimal calcularDesconto(DadosCupom dados) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemCarrinho item : dados.itens()) {
            int gratis = unidadesGratis(item);
            desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
        }
        return Moeda.emCentavos(desconto);
    }

    private int unidadesGratis(ItemCarrinho item) {
        return item.quantidade() / UNIDADES_POR_TRIO;
    }
}
