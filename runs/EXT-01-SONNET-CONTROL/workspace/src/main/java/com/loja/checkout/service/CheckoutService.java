package com.loja.checkout.service;

import com.loja.checkout.dto.ItemPedidoRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.enums.Cupom;
import com.loja.checkout.enums.FormaPagamento;
import com.loja.checkout.enums.ModalidadeEntrega;
import com.loja.checkout.enums.NivelClube;
import com.loja.checkout.enums.PagamentoResultado;
import com.loja.checkout.enums.Regiao;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CheckoutService {

    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000.00");

    public ResumoResponse calcularResumo(ResumoRequest request) {
        BigDecimal subtotalProdutos = validarItensECalcularSubtotal(request.itens());
        BigDecimal pesoTotal = calcularPesoTotal(request.itens());

        NivelClube nivelClube = parseEnum(NivelClube.class, request.nivelClube(), CodigoErro.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = parseEnum(Regiao.class, request.regiao(), CodigoErro.REGIAO_INVALIDA);
        ModalidadeEntrega modalidade = parseEnum(ModalidadeEntrega.class, request.modalidadeEntrega(), CodigoErro.MODALIDADE_INVALIDA);

        if (!modalidade.disponivelPara(pesoTotal)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
        BigDecimal freteBruto = Dinheiro.arredondar(modalidade.calcularCusto(pesoTotal));
        BigDecimal frete = nivelClube.isFreteGratis() ? BigDecimal.ZERO : freteBruto;

        Cupom cupom = null;
        if (request.cupom() != null && !request.cupom().isBlank()) {
            cupom = parseEnum(Cupom.class, request.cupom(), CodigoErro.CUPOM_INVALIDO);
            if (!cupom.aplicavel(request.itens(), subtotalProdutos)) {
                throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
            }
        }
        BigDecimal descontoCupom = cupom != null
                ? cupom.calcularDesconto(request.itens(), subtotalProdutos, frete)
                : BigDecimal.ZERO;

        BigDecimal imposto = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).multiply(regiao.getAliquotaImposto()));

        BigDecimal totalPedidoSemImposto = subtotalProdutos.subtract(descontoCupom).add(frete);
        BigDecimal totalPedido = totalPedidoSemImposto.add(imposto);

        FormaPagamento formaPagamento = parseEnum(FormaPagamento.class, request.formaPagamento(), CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = request.parcelas() != null ? request.parcelas() : 1;
        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!formaPagamento.disponivelPara(totalPedidoSemImposto)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        PagamentoResultado pagamento = formaPagamento.calcular(totalPedido, parcelas);

        BigDecimal creditoProximaCompra = Dinheiro.arredondar(
                subtotalProdutos.multiply(nivelClube.getPercentualCredito()));
        boolean brinde = nivelClube.temDireitoABrinde(subtotalProdutos);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.getPrazoDias(),
                imposto,
                pagamento.ajuste(),
                pagamento.totalFinal(),
                parcelas,
                pagamento.valorParcela(),
                creditoProximaCompra,
                brinde
        );
    }

    private BigDecimal validarItensECalcularSubtotal(List<ItemPedidoRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemPedidoRequest item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
            }
            subtotal = subtotal.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return Dinheiro.arredondar(subtotal);
    }

    private BigDecimal calcularPesoTotal(List<ItemPedidoRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemPedidoRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
    }

    private <T extends Enum<T>> T parseEnum(Class<T> tipo, String valor, CodigoErro erroSeInvalido) {
        if (valor == null || valor.isBlank()) {
            throw new CheckoutException(erroSeInvalido);
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException ex) {
            throw new CheckoutException(erroSeInvalido);
        }
    }
}
