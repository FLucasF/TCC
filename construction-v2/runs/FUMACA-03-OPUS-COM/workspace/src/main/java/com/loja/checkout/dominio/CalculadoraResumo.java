package com.loja.checkout.dominio;

import com.loja.checkout.api.CheckoutException;
import com.loja.checkout.api.CodigoErro;
import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.cupom.PedidoParaCupom;
import com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import com.loja.checkout.dominio.pagamento.Cobranca;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Monta o resumo da compra: a ordem das etapas e a ordem das validações são as
 * mesmas para todo pedido; o que muda de caso para caso mora na modalidade de
 * entrega, no cupom e na forma de pagamento.
 */
@Service
public class CalculadoraResumo {

    public ResumoResponse calcular(ResumoRequest pedido) {
        List<ItemPedido> itens = itensValidos(pedido);

        ModalidadeEntrega modalidade = ModalidadeEntrega.porCodigo(pedido.modalidadeEntrega())
                .orElseThrow(() -> new CheckoutException(CodigoErro.MODALIDADE_INVALIDA));
        BigDecimal pesoKg = somar(itens, ItemPedido::peso);
        if (!modalidade.atende(pesoKg)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotalProdutos = Dinheiro.valor(somar(itens, ItemPedido::total));
        BigDecimal frete = modalidade.frete(pesoKg);
        BigDecimal descontoCupom = descontoDoCupom(pedido.cupom(),
                new PedidoParaCupom(itens, subtotalProdutos, frete));

        BigDecimal totalPedido = Dinheiro.valor(subtotalProdutos.subtract(descontoCupom).add(frete));

        FormaPagamento formaPagamento = FormaPagamento.porCodigo(pedido.formaPagamento())
                .orElseThrow(() -> new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA));
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        if (!formaPagamento.parcelasPermitidas(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!formaPagamento.atende(totalPedido)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        Cobranca cobranca = formaPagamento.cobranca(totalPedido, parcelas);
        BigDecimal ajustePagamento = Dinheiro.valor(cobranca.totalFinal().subtract(totalPedido));

        return new ResumoResponse(subtotalProdutos, descontoCupom, frete, modalidade.prazoDias(),
                ajustePagamento, cobranca.totalFinal(), parcelas, cobranca.valorParcela());
    }

    private List<ItemPedido> itensValidos(ResumoRequest pedido) {
        if (pedido == null || pedido.itens() == null || pedido.itens().isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        return pedido.itens().stream().map(this::itemValido).toList();
    }

    private ItemPedido itemValido(ItemRequest item) {
        if (item == null || !positivo(item.precoUnitario()) || !positivo(item.pesoKg())
                || item.quantidade() == null || item.quantidade() <= 0) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal descontoDoCupom(String codigo, PedidoParaCupom pedido) {
        if (codigo == null) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = Cupom.porCodigo(codigo)
                .orElseThrow(() -> new CheckoutException(CodigoErro.CUPOM_INVALIDO));
        if (!cupom.aplicavel(pedido)) {
            throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return cupom.desconto(pedido);
    }

    private BigDecimal somar(List<ItemPedido> itens, java.util.function.Function<ItemPedido, BigDecimal> parcela) {
        return itens.stream().map(parcela).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
