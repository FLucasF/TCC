package com.loja.checkout.service;

import com.loja.checkout.domain.*;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

import static com.loja.checkout.domain.Arredondamento.*;

@Service
public class CheckoutService {

    public ResumoResponse calcular(PedidoRequest req) {
        validarItens(req.itens());

        NivelClube clube = parsearEnum(NivelClube.class, req.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = parsearEnum(Regiao.class, req.regiao(), "REGIAO_INVALIDA");
        ModalidadeEntrega modalidade = parsearEnum(ModalidadeEntrega.class, req.modalidadeEntrega(), "MODALIDADE_INVALIDA");

        BigDecimal subtotal = r(req.itens().stream()
                .map(i -> i.precoUnitario().multiply(BigDecimal.valueOf(i.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        BigDecimal pesoTotal = req.itens().stream()
                .map(i -> i.pesoKg().multiply(BigDecimal.valueOf(i.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        modalidade.validarDisponibilidade(pesoTotal);

        BigDecimal frete = clube.freteGratis() ? ZERO : modalidade.calcularFrete(pesoTotal);

        BigDecimal descontoCupom = calcularDescontoCupom(req, subtotal, frete);

        FormaPagamento pagamento = parsearEnum(FormaPagamento.class, req.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = req.parcelas() != null ? req.parcelas() : 1;
        pagamento.validarParcelas(parcelas);

        BigDecimal seguro = regiao.calcularSeguro(subtotal);
        BigDecimal totalPedido = r(subtotal.subtract(descontoCupom).add(frete).add(seguro));

        pagamento.validarDisponibilidade(totalPedido);

        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoEntregaDias(),
                seguro,
                resultado.ajustePagamento(),
                resultado.totalFinal(),
                resultado.parcelas(),
                resultado.valorParcela(),
                clube.calcularCredito(subtotal),
                clube.darBrinde(subtotal));
    }

    private BigDecimal calcularDescontoCupom(PedidoRequest req, BigDecimal subtotal, BigDecimal frete) {
        if (req.cupom() == null || req.cupom().isBlank()) return ZERO;

        Cupom cupom = parsearEnum(Cupom.class, req.cupom(), "CUPOM_INVALIDO");
        cupom.validarAplicabilidade(subtotal);

        List<ItemValor> itensValor = req.itens().stream()
                .map(i -> new ItemValor(i.precoUnitario(), i.quantidade()))
                .toList();

        return cupom.calcularDesconto(subtotal, frete, itensValor);
    }

    private void validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) throw new CheckoutException("PEDIDO_INVALIDO");
        for (ItemRequest item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private static <T extends Enum<T>> T parsearEnum(Class<T> cls, String valor, String codigoErro) {
        if (valor == null || valor.isBlank()) throw new CheckoutException(codigoErro);
        try {
            return Enum.valueOf(cls, valor.trim());
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(codigoErro);
        }
    }
}
