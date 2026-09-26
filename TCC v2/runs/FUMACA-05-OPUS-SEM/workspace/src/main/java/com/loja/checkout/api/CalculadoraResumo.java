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
import com.loja.checkout.pagamento.CatalogoPagamentos;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Monta o resumo da compra na ordem combinada: produtos, cupom, frete,
 * total do pedido e ajuste da forma de pagamento.
 */
@Service
public class CalculadoraResumo {

    private final CatalogoEntregas entregas;
    private final CatalogoCupons cupons;
    private final CatalogoPagamentos pagamentos;

    public CalculadoraResumo(CatalogoEntregas entregas,
                             CatalogoCupons cupons,
                             CatalogoPagamentos pagamentos) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.pagamentos = pagamentos;
    }

    public RespostaResumo calcular(RequisicaoResumo requisicao) {
        if (requisicao == null) {
            throw new CheckoutException(ErroCheckout.PEDIDO_INVALIDO);
        }

        // 1. Produtos
        Pedido pedido = validarCarrinho(requisicao.itens());

        // 2. Entrega (o frete e calculado antes do cupom porque FRETEGRATIS depende dele)
        ModalidadeEntrega modalidade = entregas.buscar(requisicao.modalidadeEntrega())
                .orElseThrow(() -> new CheckoutException(ErroCheckout.MODALIDADE_INVALIDA));
        if (!modalidade.atende(pedido)) {
            throw new CheckoutException(ErroCheckout.MODALIDADE_INDISPONIVEL);
        }
        BigDecimal frete = Dinheiro.centavos(modalidade.frete(pedido));

        // 3. Cupom
        BigDecimal descontoCupom = calcularDesconto(requisicao.cupom(), pedido, frete);

        // 4. Total do pedido
        BigDecimal totalPedido = Dinheiro.centavos(
                pedido.subtotalProdutos().subtract(descontoCupom).add(frete));

        // 5. Pagamento
        FormaPagamento formaPagamento = pagamentos.buscar(requisicao.formaPagamento())
                .orElseThrow(() -> new CheckoutException(ErroCheckout.FORMA_PAGAMENTO_INVALIDA));
        int parcelas = requisicao.parcelas() == null ? 1 : requisicao.parcelas();
        if (!formaPagamento.permiteParcelamento(parcelas)) {
            throw new CheckoutException(ErroCheckout.PARCELAMENTO_INVALIDO);
        }
        if (!formaPagamento.atende(totalPedido)) {
            throw new CheckoutException(ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        ResultadoPagamento pagamento = formaPagamento.calcular(totalPedido, parcelas);

        BigDecimal ajuste = Dinheiro.centavos(pagamento.totalFinal().subtract(totalPedido));

        return new RespostaResumo(
                pedido.subtotalProdutos(),
                descontoCupom,
                frete,
                modalidade.prazoEntregaDias(),
                ajuste,
                Dinheiro.centavos(pagamento.totalFinal()),
                pagamento.parcelas(),
                Dinheiro.centavos(pagamento.valorParcela()));
    }

    private Pedido validarCarrinho(List<ItemRequisicao> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException(ErroCheckout.PEDIDO_INVALIDO);
        }
        List<ItemPedido> validados = new ArrayList<>(itens.size());
        for (ItemRequisicao item : itens) {
            if (item == null
                    || naoPositivo(item.precoUnitario())
                    || item.quantidade() == null || item.quantidade() <= 0
                    || naoPositivo(item.pesoKg())) {
                throw new CheckoutException(ErroCheckout.PEDIDO_INVALIDO);
            }
            validados.add(new ItemPedido(
                    item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return Pedido.de(validados);
    }

    private boolean naoPositivo(BigDecimal valor) {
        return valor == null || valor.compareTo(BigDecimal.ZERO) <= 0;
    }

    private BigDecimal calcularDesconto(String codigo, Pedido pedido, BigDecimal frete) {
        if (codigo == null || codigo.isBlank()) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(codigo)
                .orElseThrow(() -> new CheckoutException(ErroCheckout.CUPOM_INVALIDO));
        ContextoCupom contexto = new ContextoCupom(pedido, frete);
        if (!cupom.aplicavel(contexto)) {
            throw new CheckoutException(ErroCheckout.CUPOM_NAO_APLICAVEL);
        }
        return Dinheiro.centavos(cupom.desconto(contexto));
    }
}
