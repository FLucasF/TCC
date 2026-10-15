package com.loja.checkout.service;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.enums.ContextoCupom;
import com.loja.checkout.enums.Cupom;
import com.loja.checkout.enums.FormaPagamento;
import com.loja.checkout.enums.ModalidadeEntrega;
import com.loja.checkout.enums.NivelClube;
import com.loja.checkout.enums.Regiao;
import com.loja.checkout.enums.ResultadoPagamento;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CheckoutService {

    public ResumoResponse calcularResumo(ResumoRequest request) {
        List<ItemRequest> itens = validarItens(request.itens());
        NivelClube nivelClube = validarNivelClube(request.nivelClube());
        Regiao regiao = validarRegiao(request.regiao());
        ModalidadeEntrega modalidade = validarModalidade(request.modalidadeEntrega());

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(itens);
        BigDecimal pesoTotalKg = calcularPesoTotal(itens);

        if (!modalidade.disponivel(pesoTotalKg)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal frete = nivelClube.freteGratis()
                ? BigDecimal.ZERO.setScale(2)
                : modalidade.calcularFrete(pesoTotalKg);

        Cupom cupom = validarCupom(request.cupom());
        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (cupom != null) {
            ContextoCupom contextoCupom = new ContextoCupom(itens, subtotalProdutos, frete);
            if (!cupom.aplicavel(contextoCupom)) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = cupom.calcularDesconto(contextoCupom);
        }

        FormaPagamento formaPagamento = validarFormaPagamento(request.formaPagamento());
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal totalPedidoSemImposto = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(frete));

        if (!formaPagamento.disponivel(totalPedidoSemImposto)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        BigDecimal imposto = Dinheiro.arredondar(
                regiao.aliquota().multiply(subtotalProdutos.subtract(descontoCupom)));

        BigDecimal totalPedidoComImposto = Dinheiro.arredondar(totalPedidoSemImposto.add(imposto));

        ResultadoPagamento resultadoPagamento = formaPagamento.calcular(totalPedidoComImposto, parcelas);

        BigDecimal creditoProximaCompra = Dinheiro.arredondar(
                subtotalProdutos.multiply(nivelClube.percentualCredito()));
        boolean brinde = nivelClube.elegivelBrinde(subtotalProdutos);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                imposto,
                resultadoPagamento.ajuste(),
                resultadoPagamento.totalFinal(),
                parcelas,
                resultadoPagamento.valorParcela(),
                creditoProximaCompra,
                brinde
        );
    }

    private List<ItemRequest> validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemRequest item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().signum() <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().signum() <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
        return itens;
    }

    private NivelClube validarNivelClube(String valor) {
        return parseEnum(NivelClube.class, valor, "NIVEL_CLUBE_INVALIDO");
    }

    private Regiao validarRegiao(String valor) {
        return parseEnum(Regiao.class, valor, "REGIAO_INVALIDA");
    }

    private ModalidadeEntrega validarModalidade(String valor) {
        return parseEnum(ModalidadeEntrega.class, valor, "MODALIDADE_INVALIDA");
    }

    private FormaPagamento validarFormaPagamento(String valor) {
        return parseEnum(FormaPagamento.class, valor, "FORMA_PAGAMENTO_INVALIDA");
    }

    private Cupom validarCupom(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Cupom.valueOf(valor);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> tipo, String valor, String codigoErro) {
        if (valor == null || valor.isBlank()) {
            throw new CheckoutException(codigoErro);
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(codigoErro);
        }
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return Dinheiro.arredondar(subtotal);
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
    }
}
