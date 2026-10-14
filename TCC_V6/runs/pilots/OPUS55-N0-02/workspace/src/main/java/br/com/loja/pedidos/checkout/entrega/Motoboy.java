package br.com.loja.pedidos.checkout.entrega;

import br.com.loja.pedidos.comum.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal FRETE = Dinheiro.reais("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public CotacaoEntrega cotar(PedidoParaEntrega pedido) {
        if (pedido.pesoKg().compareTo(PESO_MAXIMO_KG) > 0) {
            throw Indisponivel.modalidade();
        }
        return new CotacaoEntrega(FRETE, 0);
    }

    /** O motoboy é terceirizado: o frete é cobrado mesmo de quem tem frete grátis no clube. */
    @Override
    public boolean aceitaFreteGratisDoClube() {
        return false;
    }
}
