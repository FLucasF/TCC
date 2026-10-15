package com.loja.checkout.service;

import com.loja.checkout.club.NivelClube;
import com.loja.checkout.club.NivelClubeRegistry;
import com.loja.checkout.coupon.Cupom;
import com.loja.checkout.coupon.CupomRegistry;
import com.loja.checkout.delivery.ModalidadeEntrega;
import com.loja.checkout.delivery.ModalidadeEntregaRegistry;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CodigoErro;
import com.loja.checkout.exception.PedidoRecusadoException;
import com.loja.checkout.payment.FormaPagamento;
import com.loja.checkout.payment.FormaPagamentoRegistry;
import com.loja.checkout.payment.ResultadoPagamento;
import com.loja.checkout.region.Regiao;
import com.loja.checkout.util.Dinheiro;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {

    private final ModalidadeEntregaRegistry modalidadeEntregaRegistry;
    private final CupomRegistry cupomRegistry;
    private final NivelClubeRegistry nivelClubeRegistry;
    private final FormaPagamentoRegistry formaPagamentoRegistry;

    public CheckoutService(
            ModalidadeEntregaRegistry modalidadeEntregaRegistry,
            CupomRegistry cupomRegistry,
            NivelClubeRegistry nivelClubeRegistry,
            FormaPagamentoRegistry formaPagamentoRegistry) {
        this.modalidadeEntregaRegistry = modalidadeEntregaRegistry;
        this.cupomRegistry = cupomRegistry;
        this.nivelClubeRegistry = nivelClubeRegistry;
        this.formaPagamentoRegistry = formaPagamentoRegistry;
    }

    public ResumoResponse calcularResumo(ResumoRequest request) {
        validarItens(request.itens());

        NivelClube nivelClube = nivelClubeRegistry.buscar(request.nivelClube())
                .orElseThrow(() -> new PedidoRecusadoException(CodigoErro.NIVEL_CLUBE_INVALIDO));

        Regiao regiao = Regiao.deCodigo(request.regiao());
        if (regiao == null) {
            throw new PedidoRecusadoException(CodigoErro.REGIAO_INVALIDA);
        }

        ModalidadeEntrega modalidadeEntrega = modalidadeEntregaRegistry.buscar(request.modalidadeEntrega())
                .orElseThrow(() -> new PedidoRecusadoException(CodigoErro.MODALIDADE_INVALIDA));

        BigDecimal pesoTotalKg = calcularPesoTotalKg(request.itens());
        if (!modalidadeEntrega.disponivelPara(pesoTotalKg)) {
            throw new PedidoRecusadoException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotalProdutos = Dinheiro.arredondar(calcularSubtotalProdutos(request.itens()));

        Cupom cupom = null;
        if (request.cupom() != null && !request.cupom().isBlank()) {
            cupom = cupomRegistry.buscar(request.cupom())
                    .orElseThrow(() -> new PedidoRecusadoException(CodigoErro.CUPOM_INVALIDO));
            if (!cupom.aplicavelPara(request.itens(), subtotalProdutos)) {
                throw new PedidoRecusadoException(CodigoErro.CUPOM_NAO_APLICAVEL);
            }
        }

        FormaPagamento formaPagamento = formaPagamentoRegistry.buscar(request.formaPagamento())
                .orElseThrow(() -> new PedidoRecusadoException(CodigoErro.FORMA_PAGAMENTO_INVALIDA));

        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!formaPagamento.aceitaParcelas(parcelas)) {
            throw new PedidoRecusadoException(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        BigDecimal freteBase = Dinheiro.arredondar(modalidadeEntrega.calcularFrete(pesoTotalKg));
        BigDecimal frete = nivelClube.isentoDeFrete() ? BigDecimal.ZERO.setScale(2) : freteBase;

        BigDecimal descontoCupom = cupom == null
                ? BigDecimal.ZERO.setScale(2)
                : Dinheiro.arredondar(cupom.calcularDesconto(request.itens(), subtotalProdutos, frete));

        BigDecimal seguro = Dinheiro.arredondar(regiao.calcularSeguro(subtotalProdutos));

        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        if (!formaPagamento.disponivelPara(totalPedido)) {
            throw new PedidoRecusadoException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento resultadoPagamento = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal totalFinal = Dinheiro.arredondar(resultadoPagamento.totalFinal());
        BigDecimal valorParcela = Dinheiro.arredondar(resultadoPagamento.valorParcela());
        BigDecimal ajustePagamento = totalFinal.subtract(totalPedido);

        BigDecimal creditoProximaCompra = Dinheiro.arredondar(nivelClube.calcularCredito(subtotalProdutos));
        boolean brinde = nivelClube.temBrinde(subtotalProdutos);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidadeEntrega.prazoEntregaDias(),
                seguro,
                ajustePagamento,
                totalFinal,
                parcelas,
                valorParcela,
                creditoProximaCompra,
                brinde);
    }

    private void validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
        }
        for (ItemRequest item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().signum() <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().signum() <= 0) {
                throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
            }
        }
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return subtotal;
    }

    private BigDecimal calcularPesoTotalKg(List<ItemRequest> itens) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            pesoTotal = pesoTotal.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return pesoTotal;
    }
}
