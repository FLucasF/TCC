package br.com.loja.pedidos.pagamento;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.com.loja.pedidos.comum.Dinheiro;

/** À vista, com 5% de desconto no total para quem não usou cupom. */
@Component
public class Pix implements FormaPagamento {

    private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("5");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas, boolean usouCupom) {
        if (usouCupom) {
            return ResultadoPagamento.aVista(totalPedido);
        }
        return ResultadoPagamento.aVista(totalPedido.subtract(Dinheiro.porcentagem(totalPedido, PERCENTUAL_DESCONTO)));
    }
}
