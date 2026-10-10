package br.com.loja.checkout.dominio.pagamento;

import org.springframework.stereotype.Component;

import br.com.loja.checkout.dominio.Dinheiro;

/** Sempre a vista, com a tarifa do banco, e nao vale acima de R$ 1.000,00. */
@Component
public class Boleto implements FormaPagamento {

    private static final Dinheiro TARIFA = Dinheiro.de("3.49");
    private static final Dinheiro TOTAL_MAXIMO = Dinheiro.de("1000.00");

    @Override
    public String codigo() {
        return "BOLETO";
    }

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean atende(Dinheiro totalPedido) {
        return !totalPedido.maiorQue(TOTAL_MAXIMO);
    }

    @Override
    public Cobranca cobrar(Dinheiro totalPedido, int parcelas) {
        Dinheiro totalFinal = totalPedido.mais(TARIFA);
        return new Cobranca(totalFinal, totalFinal);
    }
}
