package br.com.loja.checkout.dominio.cupom;

import org.springframework.stereotype.Component;

import br.com.loja.checkout.dominio.Dinheiro;

/** R$ 50,00 nos produtos, so para compras a partir de R$ 300,00 em produtos. */
@Component
public class Menos50 implements Cupom {

    private static final Dinheiro DESCONTO = Dinheiro.de("50.00");
    private static final Dinheiro MINIMO_PRODUTOS = Dinheiro.de("300.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(ContextoCupom contexto) {
        return !contexto.subtotalProdutos().menorQue(MINIMO_PRODUTOS);
    }

    @Override
    public Dinheiro desconto(ContextoCupom contexto) {
        return DESCONTO;
    }
}
