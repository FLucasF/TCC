package com.loja.checkout.api;

import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.Cupons;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.entrega.Entregas;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.Pagamentos;
import com.loja.checkout.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Calcula o resumo da compra na finalizacao do pedido. */
@Service
public class ResumoCheckoutService {

    private final Entregas entregas;
    private final Cupons cupons;
    private final Pagamentos pagamentos;

    public ResumoCheckoutService(Entregas entregas, Cupons cupons, Pagamentos pagamentos) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.pagamentos = pagamentos;
    }

    public ResumoResposta calcular(ResumoRequisicao requisicao) {
        Pedido pedido = montarPedido(requisicao);

        ModalidadeEntrega entrega = entregas.resolver(requisicao.modalidadeEntrega(), pedido);
        BigDecimal frete = Dinheiro.centavos(entrega.calcularFrete(pedido));

        BigDecimal descontoCupom = Dinheiro.ZERO;
        if (requisicao.cupom() != null) {
            Cupom cupom = cupons.resolver(requisicao.cupom(), pedido, frete);
            descontoCupom = Dinheiro.centavos(cupom.calcularDesconto(pedido, frete));
        }

        BigDecimal totalPedido = Dinheiro.centavos(
                pedido.subtotalProdutos().subtract(descontoCupom).add(frete));

        int parcelas = requisicao.parcelas() == null ? 1 : requisicao.parcelas();
        FormaPagamento forma =
                pagamentos.resolver(requisicao.formaPagamento(), parcelas, totalPedido);
        ResultadoPagamento pagamento = forma.calcular(totalPedido, parcelas);

        BigDecimal ajuste = Dinheiro.centavos(pagamento.totalFinal().subtract(totalPedido));

        return new ResumoResposta(
                pedido.subtotalProdutos(),
                descontoCupom,
                frete,
                entrega.prazoDias(),
                ajuste,
                pagamento.totalFinal(),
                parcelas,
                pagamento.valorParcela());
    }

    private Pedido montarPedido(ResumoRequisicao requisicao) {
        List<ResumoRequisicao.ItemRequisicao> itens = requisicao.itens();
        if (itens == null || itens.isEmpty()) {
            throw new ErroCheckout(CodigoErro.PEDIDO_INVALIDO);
        }
        List<ItemPedido> validados = new ArrayList<>(itens.size());
        for (ResumoRequisicao.ItemRequisicao item : itens) {
            if (item == null
                    || naoPositivo(item.precoUnitario())
                    || item.quantidade() == null
                    || item.quantidade() <= 0
                    || naoPositivo(item.pesoKg())) {
                throw new ErroCheckout(CodigoErro.PEDIDO_INVALIDO);
            }
            validados.add(new ItemPedido(
                    item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return Pedido.de(validados);
    }

    private boolean naoPositivo(BigDecimal valor) {
        return valor == null || valor.signum() <= 0;
    }
}
