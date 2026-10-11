package com.loja.checkout.service;

import com.loja.checkout.domain.*;
import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemDto;
import com.loja.checkout.exception.CheckoutException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
public class CheckoutService {

    public CheckoutResponse calcular(CheckoutRequest req) {
        validarItens(req.itens());

        NivelClube clube = buscarEnum(NivelClube.class, req.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = buscarEnum(Regiao.class, req.regiao(), "REGIAO_INVALIDA");
        ModalidadeEntrega entrega = buscarEnum(ModalidadeEntrega.class, req.modalidadeEntrega(), "MODALIDADE_INVALIDA");

        BigDecimal pesoTotal = calcularPesoTotal(req.itens());
        if (!entrega.isDisponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        Cupom cupom = null;
        if (req.cupom() != null) {
            cupom = buscarEnum(Cupom.class, req.cupom(), "CUPOM_INVALIDO");
        }

        BigDecimal subtotalProdutos = calcularSubtotal(req.itens());

        if (cupom != null && !cupom.isAplicavel(subtotalProdutos)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }

        FormaPagamento pagamentoEnum = buscarEnum(FormaPagamento.class, req.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");

        int parcelas = req.parcelas() != null ? req.parcelas() : 1;
        if (!pagamentoEnum.isParcelamentoValido(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal freteBase = entrega.calcularFrete(pesoTotal);
        BigDecimal frete = clube.freteGratis() ? BigDecimal.ZERO.setScale(2) : freteBase;

        BigDecimal descontoCupom = cupom != null
                ? cupom.calcularDesconto(subtotalProdutos, frete, req.itens())
                : BigDecimal.ZERO.setScale(2);

        BigDecimal seguro = regiao.calcularSeguro(subtotalProdutos);

        BigDecimal totalPedido = subtotalProdutos
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro);

        if (!pagamentoEnum.isDisponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        FormaPagamento.ResultadoPagamento resultado = pagamentoEnum.calcular(totalPedido, parcelas);

        BigDecimal credito = clube.calcularCredito(subtotalProdutos);
        boolean brinde = clube.temBrinde(subtotalProdutos);

        return new CheckoutResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                entrega.prazoEntregaDias(),
                seguro,
                resultado.ajustePagamento(),
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela(),
                credito,
                brinde
        );
    }

    private void validarItens(List<ItemDto> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemDto item : itens) {
            if (item.precoUnitario() == null || item.quantidade() == null || item.pesoKg() == null
                    || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() <= 0
                    || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private BigDecimal calcularSubtotal(List<ItemDto> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemDto item : itens) {
            subtotal = subtotal.add(
                    item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade()))
            );
        }
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularPesoTotal(List<ItemDto> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemDto item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
    }

    private <T extends Enum<T>> T buscarEnum(Class<T> clazz, String valor, String codigoErro) {
        if (valor == null) throw new CheckoutException(codigoErro);
        try {
            return Enum.valueOf(clazz, valor);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(codigoErro);
        }
    }
}
