package com.loja.checkout.servico;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.dominio.cupom.ContextoCupom;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.cupom.Cupons;
import com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import com.loja.checkout.dominio.entrega.ModalidadesEntrega;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import com.loja.checkout.dominio.pagamento.FormasPagamento;
import com.loja.checkout.dominio.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Monta o resumo da compra na ordem combinada: produtos, cupom, frete,
 * total do pedido e, por ultimo, o ajuste da forma de pagamento.
 */
@Service
public class CalculadoraResumo {

    private static final int PARCELAS_PADRAO = 1;

    private final ModalidadesEntrega modalidades;
    private final Cupons cupons;
    private final FormasPagamento formasPagamento;

    public CalculadoraResumo(ModalidadesEntrega modalidades, Cupons cupons, FormasPagamento formasPagamento) {
        this.modalidades = modalidades;
        this.cupons = cupons;
        this.formasPagamento = formasPagamento;
    }

    public ResumoResponse calcular(ResumoRequest requisicao) {
        Pedido pedido = montarPedido(requisicao);

        ModalidadeEntrega modalidade = modalidades.buscar(modalidade(requisicao))
                .orElseThrow(() -> new CheckoutException(CodigoErro.MODALIDADE_INVALIDA));
        if (!modalidade.atende(pedido)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotalProdutos = pedido.subtotalProdutos();
        BigDecimal frete = Dinheiro.arredondar(modalidade.calcularFrete(pedido));
        BigDecimal descontoCupom = calcularDescontoCupom(requisicao, pedido, subtotalProdutos, frete);

        BigDecimal totalPedido = Dinheiro.arredondar(subtotalProdutos.subtract(descontoCupom).add(frete));

        FormaPagamento formaPagamento = formasPagamento.buscar(requisicao == null ? null : requisicao.formaPagamento())
                .orElseThrow(() -> new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA));
        int parcelas = parcelas(requisicao);
        if (!formaPagamento.aceitaParcelas(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!formaPagamento.atende(totalPedido)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento pagamento = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal ajustePagamento = Dinheiro.arredondar(pagamento.totalFinal().subtract(totalPedido));

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

    private Pedido montarPedido(ResumoRequest requisicao) {
        List<ItemRequest> itens = requisicao == null ? null : requisicao.itens();
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        List<ItemPedido> itensPedido = new ArrayList<>(itens.size());
        for (ItemRequest item : itens) {
            if (item == null
                    || naoEPositivo(item.precoUnitario())
                    || item.quantidade() == null || item.quantidade() <= 0
                    || naoEPositivo(item.pesoKg())) {
                throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
            }
            itensPedido.add(new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return new Pedido(itensPedido);
    }

    private BigDecimal calcularDescontoCupom(ResumoRequest requisicao, Pedido pedido,
                                             BigDecimal subtotalProdutos, BigDecimal frete) {
        String codigo = requisicao.cupom();
        if (codigo == null || codigo.isBlank()) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(codigo)
                .orElseThrow(() -> new CheckoutException(CodigoErro.CUPOM_INVALIDO));
        ContextoCupom contexto = new ContextoCupom(pedido, subtotalProdutos, frete);
        if (!cupom.aplicavel(contexto)) {
            throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return Dinheiro.arredondar(cupom.calcularDesconto(contexto));
    }

    private String modalidade(ResumoRequest requisicao) {
        return requisicao == null ? null : requisicao.modalidadeEntrega();
    }

    private int parcelas(ResumoRequest requisicao) {
        Integer parcelas = requisicao.parcelas();
        return parcelas == null ? PARCELAS_PADRAO : parcelas;
    }

    private boolean naoEPositivo(BigDecimal valor) {
        return valor == null || valor.compareTo(BigDecimal.ZERO) <= 0;
    }
}
