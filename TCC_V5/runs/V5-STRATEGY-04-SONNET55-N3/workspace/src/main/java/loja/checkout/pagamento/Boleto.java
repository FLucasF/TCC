package loja.checkout.pagamento;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import loja.checkout.Dinheiro;

@Component
class Boleto implements FormaPagamento {

    private static final BigDecimal LIMITE = Dinheiro.valor("1000.00");
    private static final BigDecimal TARIFA = Dinheiro.valor("3.49");

    @Override
    public String codigo() {
        return "BOLETO";
    }

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return totalPedido.compareTo(LIMITE) <= 0;
    }

    @Override
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        BigDecimal valor = totalPedido.add(TARIFA);
        return new Cobranca(valor, valor);
    }
}
