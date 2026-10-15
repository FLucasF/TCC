package com.loja.checkout.service;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.clube.NiveisClubeRegistro;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.CuponsRegistro;
import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.entrega.OpcaoEntrega;
import com.loja.checkout.entrega.OpcoesEntregaRegistro;
import com.loja.checkout.erro.CheckoutException;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.FormasPagamentoRegistro;
import com.loja.checkout.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CheckoutService {

    private final OpcoesEntregaRegistro opcoesEntregaRegistro;
    private final CuponsRegistro cuponsRegistro;
    private final NiveisClubeRegistro niveisClubeRegistro;
    private final FormasPagamentoRegistro formasPagamentoRegistro;

    public CheckoutService(OpcoesEntregaRegistro opcoesEntregaRegistro,
                            CuponsRegistro cuponsRegistro,
                            NiveisClubeRegistro niveisClubeRegistro,
                            FormasPagamentoRegistro formasPagamentoRegistro) {
        this.opcoesEntregaRegistro = opcoesEntregaRegistro;
        this.cuponsRegistro = cuponsRegistro;
        this.niveisClubeRegistro = niveisClubeRegistro;
        this.formasPagamentoRegistro = formasPagamentoRegistro;
    }

    public CheckoutResponse calcularResumo(CheckoutRequest request) {
        PedidoContext pedido = validarEMontarPedido(request);

        NivelClube nivelClube = validarNivelClube(request);
        Regiao regiao = validarRegiao(request);
        OpcaoEntrega opcaoEntrega = validarModalidadeEntrega(request, pedido);

        BigDecimal freteBruto = Dinheiro.arredondar(opcaoEntrega.calcularFrete(pedido));
        BigDecimal frete = nivelClube.freteGratis() ? BigDecimal.ZERO.setScale(2) : freteBruto;

        Cupom cupom = validarCupom(request, pedido);
        BigDecimal descontoCupom = cupom == null
                ? BigDecimal.ZERO.setScale(2)
                : Dinheiro.arredondar(cupom.calcularDesconto(pedido, frete));

        BigDecimal seguro = Dinheiro.arredondar(pedido.subtotalProdutos().multiply(regiao.getPercentualSeguro()));

        BigDecimal totalPedido = pedido.subtotalProdutos()
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro);

        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        FormaPagamento formaPagamento = validarFormaPagamento(request, parcelas, totalPedido);

        ResultadoPagamento resultadoPagamento = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal ajustePagamento = resultadoPagamento.valorFinal().subtract(totalPedido);

        BigDecimal creditoProximaCompra = Dinheiro.arredondar(nivelClube.calcularCredito(pedido));
        boolean brinde = nivelClube.temDireitoABrinde(pedido);

        return new CheckoutResponse(
                pedido.subtotalProdutos(),
                descontoCupom,
                frete,
                opcaoEntrega.prazoDias(),
                seguro,
                ajustePagamento,
                resultadoPagamento.valorFinal(),
                parcelas,
                resultadoPagamento.valorParcela(),
                creditoProximaCompra,
                brinde
        );
    }

    private PedidoContext validarEMontarPedido(CheckoutRequest request) {
        List<ItemRequest> itens = request.itens();
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
            BigDecimal quantidade = BigDecimal.valueOf(item.quantidade());
            subtotal = subtotal.add(item.precoUnitario().multiply(quantidade));
            pesoTotal = pesoTotal.add(item.pesoKg().multiply(quantidade));
        }

        return new PedidoContext(itens, Dinheiro.arredondar(subtotal), pesoTotal);
    }

    private NivelClube validarNivelClube(CheckoutRequest request) {
        if (request.nivelClube() == null) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
        return niveisClubeRegistro.buscar(request.nivelClube())
                .orElseThrow(() -> new CheckoutException("NIVEL_CLUBE_INVALIDO"));
    }

    private Regiao validarRegiao(CheckoutRequest request) {
        if (request.regiao() == null) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(request.regiao());
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private OpcaoEntrega validarModalidadeEntrega(CheckoutRequest request, PedidoContext pedido) {
        if (request.modalidadeEntrega() == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        OpcaoEntrega opcaoEntrega = opcoesEntregaRegistro.buscar(request.modalidadeEntrega())
                .orElseThrow(() -> new CheckoutException("MODALIDADE_INVALIDA"));

        if (!opcaoEntrega.disponivelPara(pedido)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
        return opcaoEntrega;
    }

    private Cupom validarCupom(CheckoutRequest request, PedidoContext pedido) {
        if (request.cupom() == null || request.cupom().isBlank()) {
            return null;
        }
        Cupom cupom = cuponsRegistro.buscar(request.cupom())
                .orElseThrow(() -> new CheckoutException("CUPOM_INVALIDO"));

        if (!cupom.aplicavel(pedido)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
        return cupom;
    }

    private FormaPagamento validarFormaPagamento(CheckoutRequest request, int parcelas, BigDecimal totalPedido) {
        if (request.formaPagamento() == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        FormaPagamento formaPagamento = formasPagamentoRegistro.buscar(request.formaPagamento())
                .orElseThrow(() -> new CheckoutException("FORMA_PAGAMENTO_INVALIDA"));

        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
        if (!formaPagamento.disponivelPara(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        return formaPagamento;
    }
}
