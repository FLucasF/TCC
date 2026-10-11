package com.loja.checkout.servico;

import com.loja.checkout.api.CheckoutRequest;
import com.loja.checkout.api.CheckoutRequest.ItemRequest;
import com.loja.checkout.api.CheckoutResponse;
import com.loja.checkout.dominio.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static com.loja.checkout.dominio.CodigoErro.*;

@Service
public class CheckoutServico {

    public CheckoutResponse calcular(CheckoutRequest req) {
        validarItens(req.itens());

        NivelClube clube = parsarEnum(req.nivelClube(), NivelClube.class, NIVEL_CLUBE_INVALIDO);
        Regiao regiao = parsarEnum(req.regiao(), Regiao.class, REGIAO_INVALIDA);
        ModalidadeEntrega modalidade = parsarEnum(req.modalidadeEntrega(), ModalidadeEntrega.class, MODALIDADE_INVALIDA);

        BigDecimal pesoTotal = calcularPesoTotal(req.itens());
        modalidade.validarDisponibilidade(pesoTotal);

        Cupom cupom = null;
        if (req.cupom() != null && !req.cupom().isBlank()) {
            cupom = Cupom.fromCodigo(req.cupom())
                    .orElseThrow(() -> new CheckoutException(CUPOM_INVALIDO));
        }

        BigDecimal subtotal = calcularSubtotal(req.itens());

        BigDecimal frete = clube.isFreteGratis()
                ? BigDecimal.ZERO.setScale(2)
                : modalidade.calcularFrete(pesoTotal);

        if (cupom != null) {
            cupom.validarAplicabilidade(subtotal, frete, req.itens());
        }

        BigDecimal descontoCupom = cupom != null
                ? cupom.calcularDesconto(subtotal, frete, req.itens())
                : BigDecimal.ZERO.setScale(2);

        BigDecimal seguro = regiao.calcularSeguro(subtotal);

        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        FormaPagamento formaPagamento = parsarEnum(req.formaPagamento(), FormaPagamento.class, FORMA_PAGAMENTO_INVALIDA);

        int parcelas = req.parcelas() != null ? req.parcelas() : 1;
        formaPagamento.validarParcelas(parcelas);
        formaPagamento.validarDisponibilidade(totalPedido);

        ResultadoPagamento pagamento = formaPagamento.calcular(totalPedido, parcelas);

        BigDecimal credito = clube.calcularCredito(subtotal);
        boolean brinde = clube.temBrinde(subtotal);

        return new CheckoutResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoDias,
                seguro,
                pagamento.ajustePagamento(),
                pagamento.totalFinal(),
                pagamento.parcelas(),
                pagamento.valorParcela(),
                credito,
                brinde
        );
    }

    private <T extends Enum<T>> T parsarEnum(String valor, Class<T> tipo, CodigoErro erro) {
        if (valor == null || valor.isBlank()) throw new CheckoutException(erro);
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(erro);
        }
    }

    private void validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) throw new CheckoutException(PEDIDO_INVALIDO);
        for (ItemRequest item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0)
                throw new CheckoutException(PEDIDO_INVALIDO);
            if (item.quantidade() == null || item.quantidade() <= 0)
                throw new CheckoutException(PEDIDO_INVALIDO);
            if (item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0)
                throw new CheckoutException(PEDIDO_INVALIDO);
        }
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        return itens.stream()
                .map(item -> item.precoUnitario().multiply(new BigDecimal(item.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        return itens.stream()
                .map(item -> item.pesoKg().multiply(new BigDecimal(item.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
