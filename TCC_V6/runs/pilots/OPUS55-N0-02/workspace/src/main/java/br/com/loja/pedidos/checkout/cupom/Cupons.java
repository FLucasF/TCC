package br.com.loja.pedidos.checkout.cupom;

import br.com.loja.pedidos.comum.Dinheiro;
import br.com.loja.pedidos.comum.ErroNegocio;
import br.com.loja.pedidos.comum.Registro;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class Cupons {

    /** Nenhum cupom dá mais que 30% do valor dos produtos. */
    private static final BigDecimal LIMITE_PERCENTUAL = new BigDecimal("30");

    private final Registro<Cupom> registro;

    public Cupons(List<Cupom> cupons) {
        this.registro = new Registro<>(cupons, Cupom::codigo);
    }

    /** Desconto do cupom informado, já limitado a 30% dos produtos. */
    public BigDecimal desconto(String codigo, PedidoParaCupom pedido) {
        Cupom cupom = registro.buscar(codigo).orElseThrow(() -> ErroNegocio.naoProcessavel("CUPOM_INVALIDO"));
        BigDecimal limite = Dinheiro.percentual(pedido.subtotalProdutos(), LIMITE_PERCENTUAL);
        return Dinheiro.menor(Dinheiro.arredondar(cupom.desconto(pedido)), limite);
    }
}
