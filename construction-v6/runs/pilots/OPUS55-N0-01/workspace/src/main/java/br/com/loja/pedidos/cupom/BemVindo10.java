package br.com.loja.pedidos.cupom;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.com.loja.pedidos.cliente.HistoricoCompras;
import br.com.loja.pedidos.comum.Dinheiro;

/** 10% nos produtos, só na primeira compra. */
@Component
public class BemVindo10 implements Cupom {

    private final HistoricoCompras historico;

    public BemVindo10(HistoricoCompras historico) {
        this.historico = historico;
    }

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        if (historico.jaComprou(contexto.cpf())) {
            throw Cupom.naoAplicavel();
        }
        return Dinheiro.porcentagem(contexto.subtotalProdutos(), BigDecimal.TEN);
    }
}
