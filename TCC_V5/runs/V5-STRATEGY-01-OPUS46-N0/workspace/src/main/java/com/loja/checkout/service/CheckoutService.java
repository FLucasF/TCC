package com.loja.checkout.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.model.Cupom;
import com.loja.checkout.model.FormaPagamento;
import com.loja.checkout.model.FormaPagamento.ResultadoPagamento;
import com.loja.checkout.model.ModalidadeEntrega;
import com.loja.checkout.model.NivelClube;
import com.loja.checkout.model.Regiao;

@Service
public class CheckoutService {

    public ResumoResponse calcular(PedidoRequest pedido) {
        validarItens(pedido.itens());

        NivelClube nivelClube = parseEnum(NivelClube.class, pedido.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = parseEnum(Regiao.class, pedido.regiao(), "REGIAO_INVALIDA");
        ModalidadeEntrega modalidade = parseEnum(ModalidadeEntrega.class, pedido.modalidadeEntrega(), "MODALIDADE_INVALIDA");

        List<ItemRequest> itens = pedido.itens();
        BigDecimal subtotal = calcularSubtotal(itens);
        BigDecimal pesoTotal = calcularPeso(itens);

        if (!modalidade.disponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        Cupom cupom = null;
        if (pedido.cupom() != null && !pedido.cupom().isBlank()) {
            cupom = parseEnum(Cupom.class, pedido.cupom(), "CUPOM_INVALIDO");
            if (!cupom.aplicavel(subtotal, itens)) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
        }

        FormaPagamento formaPagamento = parseEnum(FormaPagamento.class, pedido.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = pedido.parcelas() != null ? pedido.parcelas() : 1;

        if (!formaPagamento.parcelamentoValido(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal frete = modalidade.calcularFrete(pesoTotal);
        int prazo = modalidade.prazoDias();

        if (nivelClube.isFreteGratis()) {
            frete = BigDecimal.ZERO.setScale(2);
        }

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (cupom != null) {
            descontoCupom = cupom.calcularDesconto(subtotal, frete, itens);
        }

        BigDecimal seguro = regiao.calcularSeguro(subtotal);

        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro)
                .setScale(2, RoundingMode.HALF_EVEN);

        if (!formaPagamento.disponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultado = formaPagamento.calcular(totalPedido, parcelas);

        BigDecimal credito = nivelClube.calcularCredito(subtotal);
        boolean brinde = nivelClube.temBrinde(subtotal);

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                prazo,
                seguro,
                resultado.ajustePagamento(),
                resultado.totalFinal(),
                resultado.parcelas(),
                resultado.valorParcela(),
                credito,
                brinde
        );
    }

    private void validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemRequest item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            total = total.add(item.precoUnitario().multiply(new BigDecimal(item.quantidade())));
        }
        return total.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularPeso(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(new BigDecimal(item.quantidade())));
        }
        return peso;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String codigoErro) {
        if (value == null || value.isBlank()) {
            throw new CheckoutException(codigoErro);
        }
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(codigoErro);
        }
    }
}
