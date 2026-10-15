package com.loja.checkout.api;

import com.loja.checkout.cupom.CatalogoCupons;
import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.entrega.CatalogoEntregas;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.CatalogoFormasPagamento;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

/** Calcula o resumo da compra na finalizacao do pedido. */
@Service
public class CheckoutService {

    private static final int PARCELAS_PADRAO = 1;

    private final CatalogoEntregas entregas;
    private final CatalogoCupons cupons;
    private final CatalogoFormasPagamento formasPagamento;

    public CheckoutService(CatalogoEntregas entregas, CatalogoCupons cupons,
                           CatalogoFormasPagamento formasPagamento) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.formasPagamento = formasPagamento;
    }

    public ResumoResponse calcular(ResumoRequest requisicao) {
        if (requisicao == null) {
            throw new CheckoutException(ErroCheckout.PEDIDO_INVALIDO);
        }

        Pedido pedido = montarPedido(requisicao.itens());
        ModalidadeEntrega entrega = escolherEntrega(requisicao.modalidadeEntrega(), pedido);

        BigDecimal subtotalProdutos = pedido.subtotalProdutos();
        BigDecimal frete = entrega.frete(pedido);
        BigDecimal descontoCupom = calcularDesconto(
                requisicao.cupom(), new ContextoCupom(pedido, subtotalProdutos, frete));

        FormaPagamento pagamento = escolherPagamento(requisicao.formaPagamento());
        int parcelas = requisicao.parcelas() == null ? PARCELAS_PADRAO : requisicao.parcelas();
        if (!pagamento.aceitaParcelas(parcelas)) {
            throw new CheckoutException(ErroCheckout.PARCELAMENTO_INVALIDO);
        }

        BigDecimal totalPedido = Dinheiro.centavos(
                subtotalProdutos.subtract(descontoCupom).add(frete));
        if (!pagamento.atende(totalPedido)) {
            throw new CheckoutException(ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);
        BigDecimal ajustePagamento = Dinheiro.centavos(
                resultado.totalFinal().subtract(totalPedido));

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                entrega.prazoEntregaDias(),
                ajustePagamento,
                resultado.totalFinal(),
                resultado.parcelas(),
                resultado.valorParcela());
    }

    private Pedido montarPedido(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException(ErroCheckout.PEDIDO_INVALIDO);
        }
        return new Pedido(itens.stream().map(this::converterItem).toList());
    }

    private ItemPedido converterItem(ItemRequest item) {
        if (item == null
                || naoEPositivo(item.precoUnitario())
                || item.quantidade() == null || item.quantidade() <= 0
                || naoEPositivo(item.pesoKg())) {
            throw new CheckoutException(ErroCheckout.PEDIDO_INVALIDO);
        }
        return new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private boolean naoEPositivo(BigDecimal valor) {
        return valor == null || valor.compareTo(BigDecimal.ZERO) <= 0;
    }

    private ModalidadeEntrega escolherEntrega(String codigo, Pedido pedido) {
        ModalidadeEntrega entrega = entregas.buscar(codigo)
                .orElseThrow(() -> new CheckoutException(ErroCheckout.MODALIDADE_INVALIDA));
        if (!entrega.atende(pedido)) {
            throw new CheckoutException(ErroCheckout.MODALIDADE_INDISPONIVEL);
        }
        return entrega;
    }

    /** Sem cupom o desconto e zero; com cupom, vale um cupom por pedido. */
    private BigDecimal calcularDesconto(String codigo, ContextoCupom contexto) {
        if (codigo == null || codigo.isBlank()) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(codigo)
                .orElseThrow(() -> new CheckoutException(ErroCheckout.CUPOM_INVALIDO));
        if (!cupom.aplicavel(contexto)) {
            throw new CheckoutException(ErroCheckout.CUPOM_NAO_APLICAVEL);
        }
        return cupom.desconto(contexto);
    }

    private FormaPagamento escolherPagamento(String codigo) {
        return formasPagamento.buscar(codigo)
                .orElseThrow(() -> new CheckoutException(ErroCheckout.FORMA_PAGAMENTO_INVALIDA));
    }
}
