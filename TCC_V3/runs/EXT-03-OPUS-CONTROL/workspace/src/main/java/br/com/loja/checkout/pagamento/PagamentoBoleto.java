package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Tarifa bancaria de R$ 3,49 somada ao total, sempre a vista. Nao e aceito quando o
 * total do pedido (produtos - cupom + frete) passa de R$ 1.000,00.
 */
@Component
public class PagamentoBoleto implements FormaPagamento {

    private static final BigDecimal TARIFA = Dinheiro.de("3.49");
    private static final BigDecimal TETO = Dinheiro.de("1000.00");

    @Override
    public String codigo() {
        return "BOLETO";
    }

    @Override
    public boolean parcelamentoPermitido(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean atende(ContextoPagamento contexto) {
        return contexto.totalSemImposto().compareTo(TETO) <= 0;
    }

    @Override
    public ResultadoPagamento aplicar(ContextoPagamento contexto) {
        BigDecimal totalFinal = Dinheiro.centavos(contexto.totalPedido().add(TARIFA));
        return new ResultadoPagamento(totalFinal, totalFinal);
    }
}
