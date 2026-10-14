package br.com.loja.pedidos.checkout.entrega;

import br.com.loja.pedidos.comum.Dinheiro;
import org.springframework.stereotype.Component;

@Component
public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public CotacaoEntrega cotar(PedidoParaEntrega pedido) {
        return new CotacaoEntrega(Dinheiro.ZERO, 1);
    }
}
