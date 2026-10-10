package br.com.loja.checkout.calculo;

import br.com.loja.checkout.pagamento.Cobranca;
import br.com.loja.checkout.pagamento.EscolhaPagamento;
import org.springframework.stereotype.Service;

@Service
public class CalculadoraResumo {

    public ResumoCompra calcular(Pedido pedido, EscolhaPagamento pagamento) {
        var subtotal = pedido.subtotalProdutos();
        var totalPedido = pedido.totalPedido();
        Cobranca cobranca = pagamento.cobrar(totalPedido);
        var ajuste = Dinheiro.centavos(cobranca.totalFinal().subtract(totalPedido));
        return new ResumoCompra(
                subtotal,
                pedido.descontoCupom(),
                pedido.frete(),
                pedido.entrega().prazoDias(),
                pedido.seguro(),
                ajuste,
                cobranca.totalFinal(),
                pagamento.parcelas(),
                cobranca.valorParcela(),
                pedido.clube().credito(subtotal),
                pedido.clube().ganhaBrinde(subtotal));
    }
}
