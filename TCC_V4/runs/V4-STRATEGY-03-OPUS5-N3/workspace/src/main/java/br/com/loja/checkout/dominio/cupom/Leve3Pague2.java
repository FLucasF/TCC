package br.com.loja.checkout.dominio.cupom;

import org.springframework.stereotype.Component;

import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Item;

/** A cada 3 unidades de um mesmo item do carrinho, uma sai de graca. */
@Component
public class Leve3Pague2 implements Cupom {

    private static final int UNIDADES_PARA_GANHAR_UMA = 3;

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    @Override
    public Dinheiro desconto(ContextoCupom contexto) {
        return contexto.itens().stream()
                .map(this::valorDasUnidadesGratis)
                .reduce(Dinheiro.ZERO, Dinheiro::mais);
    }

    private Dinheiro valorDasUnidadesGratis(Item item) {
        int gratis = item.quantidade() / UNIDADES_PARA_GANHAR_UMA;
        return Dinheiro.de(item.precoUnitario()).vezes(gratis);
    }
}
