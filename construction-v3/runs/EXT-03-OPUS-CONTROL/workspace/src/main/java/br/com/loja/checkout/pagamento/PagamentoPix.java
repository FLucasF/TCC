package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** 5% de desconto no total do pedido, sempre a vista. */
@Component
public class PagamentoPix implements FormaPagamento {

    private static final BigDecimal DESCONTO = Dinheiro.de("0.05");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    public boolean parcelamentoPermitido(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public ResultadoPagamento aplicar(ContextoPagamento contexto) {
        BigDecimal desconto = Dinheiro.percentual(contexto.totalPedido(), DESCONTO);
        BigDecimal totalFinal = Dinheiro.centavos(contexto.totalPedido().subtract(desconto));
        return new ResultadoPagamento(totalFinal, totalFinal);
    }
}
