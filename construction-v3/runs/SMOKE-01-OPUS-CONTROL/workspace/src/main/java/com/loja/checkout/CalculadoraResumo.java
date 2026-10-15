package com.loja.checkout;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.cupom.CatalogoCupons;
import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.entrega.CatalogoEntregas;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.CatalogoPagamentos;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Monta o resumo da compra: produtos, cupom, frete, total do pedido e o ajuste
 * da forma de pagamento, nessa ordem.
 */
@Service
public class CalculadoraResumo {

    private final CatalogoEntregas entregas;
    private final CatalogoCupons cupons;
    private final CatalogoPagamentos pagamentos;

    public CalculadoraResumo(CatalogoEntregas entregas, CatalogoCupons cupons, CatalogoPagamentos pagamentos) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.pagamentos = pagamentos;
    }

    public ResumoResponse calcular(ResumoRequest request) {
        Pedido pedido = lerPedido(request);

        ModalidadeEntrega modalidade = entregas.buscar(request.modalidadeEntrega())
                .orElseThrow(() -> new CheckoutException(ErroCheckout.MODALIDADE_INVALIDA));
        if (!modalidade.atende(pedido)) {
            throw new CheckoutException(ErroCheckout.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotalProdutos = pedido.subtotalProdutos();
        BigDecimal frete = modalidade.calcularFrete(pedido);
        BigDecimal descontoCupom = calcularDesconto(request.cupom(),
                new ContextoCupom(pedido, subtotalProdutos, frete));

        FormaPagamento formaPagamento = pagamentos.buscar(request.formaPagamento())
                .orElseThrow(() -> new CheckoutException(ErroCheckout.FORMA_PAGAMENTO_INVALIDA));
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!formaPagamento.aceitaParcelas(parcelas)) {
            throw new CheckoutException(ErroCheckout.PARCELAMENTO_INVALIDO);
        }

        BigDecimal totalPedido = Dinheiro.valor(subtotalProdutos.subtract(descontoCupom).add(frete));
        if (!formaPagamento.atende(totalPedido)) {
            throw new CheckoutException(ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento resultado = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal ajustePagamento = Dinheiro.valor(resultado.totalFinal().subtract(totalPedido));

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(pedido),
                ajustePagamento,
                Dinheiro.valor(resultado.totalFinal()),
                parcelas,
                Dinheiro.valor(resultado.valorParcela()));
    }

    private BigDecimal calcularDesconto(String codigo, ContextoCupom contexto) {
        if (codigo == null || codigo.isBlank()) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(codigo)
                .orElseThrow(() -> new CheckoutException(ErroCheckout.CUPOM_INVALIDO));
        if (!cupom.aplicavel(contexto)) {
            throw new CheckoutException(ErroCheckout.CUPOM_NAO_APLICAVEL);
        }
        return cupom.calcularDesconto(contexto);
    }

    private Pedido lerPedido(ResumoRequest request) {
        List<ItemRequest> itens = request == null ? null : request.itens();
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException(ErroCheckout.PEDIDO_INVALIDO);
        }
        return new Pedido(itens.stream().map(this::lerItem).toList());
    }

    private Item lerItem(ItemRequest item) {
        if (item == null
                || naoEPositivo(item.precoUnitario())
                || item.quantidade() == null || item.quantidade() <= 0
                || naoEPositivo(item.pesoKg())) {
            throw new CheckoutException(ErroCheckout.PEDIDO_INVALIDO);
        }
        return new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private boolean naoEPositivo(BigDecimal valor) {
        return valor == null || valor.compareTo(BigDecimal.ZERO) <= 0;
    }
}
