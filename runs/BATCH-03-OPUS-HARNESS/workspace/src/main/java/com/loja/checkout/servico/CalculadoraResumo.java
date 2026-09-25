package com.loja.checkout.servico;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.ContextoCupom;
import com.loja.checkout.dominio.Cupom;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.FormaPagamento;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.ModalidadeEntrega;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.dominio.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/** Monta o resumo da compra na ordem combinada com o site. */
@Service
public class CalculadoraResumo {

    private static final int PARCELAS_PADRAO = 1;

    public ResumoResponse calcular(ResumoRequest requisicao) {
        Pedido pedido = pedidoDe(requisicao.itens());

        ModalidadeEntrega modalidade = ModalidadeEntrega.porCodigo(requisicao.modalidadeEntrega())
                .orElseThrow(() -> new CheckoutException(CodigoErro.MODALIDADE_INVALIDA));
        if (!modalidade.atende(pedido)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotalProdutos = pedido.subtotalProdutos();
        BigDecimal frete = modalidade.frete(pedido);
        BigDecimal descontoCupom = descontoDe(requisicao.cupom(), new ContextoCupom(pedido, frete));

        FormaPagamento formaPagamento = FormaPagamento.porCodigo(requisicao.formaPagamento())
                .orElseThrow(() -> new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA));
        int parcelas = Objects.requireNonNullElse(requisicao.parcelas(), PARCELAS_PADRAO);
        if (!formaPagamento.aceitaParcelas(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        BigDecimal totalPedido = Dinheiro.centavos(subtotalProdutos.subtract(descontoCupom).add(frete));
        if (!formaPagamento.atende(totalPedido)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento pagamento = formaPagamento.resolver(totalPedido, parcelas);
        BigDecimal ajustePagamento = Dinheiro.centavos(pagamento.totalFinal().subtract(totalPedido));

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoEntregaDias(),
                ajustePagamento,
                pagamento.totalFinal(),
                pagamento.parcelas(),
                pagamento.valorParcela());
    }

    private Pedido pedidoDe(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new Pedido(itens.stream().map(this::itemDe).toList());
    }

    private Item itemDe(ItemRequest item) {
        if (item == null
                || naoEPositivo(item.precoUnitario())
                || item.quantidade() == null || item.quantidade() <= 0
                || naoEPositivo(item.pesoKg())) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private boolean naoEPositivo(BigDecimal valor) {
        return valor == null || valor.compareTo(BigDecimal.ZERO) <= 0;
    }

    private BigDecimal descontoDe(String codigoCupom, ContextoCupom contexto) {
        if (codigoCupom == null) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = Cupom.porCodigo(codigoCupom)
                .orElseThrow(() -> new CheckoutException(CodigoErro.CUPOM_INVALIDO));
        if (!cupom.aplicavel(contexto)) {
            throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return cupom.desconto(contexto);
    }
}
