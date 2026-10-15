package com.loja.checkout;

import com.loja.checkout.dominio.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ResumoService {

    public ResumoResponse calcular(ResumoRequest request) {
        List<ItemRequest> itens = validarItens(request.itens());
        NivelClube nivelClube = resolver(NivelClube.class, request.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = resolver(Regiao.class, request.regiao(), "REGIAO_INVALIDA");
        ModalidadeEntrega modalidade = resolver(ModalidadeEntrega.class, request.modalidadeEntrega(), "MODALIDADE_INVALIDA");

        BigDecimal pesoTotal = calcularPesoTotal(itens);
        if (!modalidade.isDisponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotal = calcularSubtotal(itens);

        Cupom cupom = resolverCupomOpcional(request.cupom());

        BigDecimal frete = modalidade.calcularFrete(pesoTotal);
        if (nivelClube.isFreteGratis()) {
            frete = new BigDecimal("0.00");
        }

        BigDecimal descontoCupom = new BigDecimal("0.00");
        if (cupom != null) {
            if (!cupom.isAplicavel(subtotal, itens)) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = cupom.calcularDesconto(subtotal, itens, frete);
        }

        BigDecimal seguro = regiao.calcularSeguro(subtotal);

        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        FormaPagamento formaPagamento = resolver(FormaPagamento.class, request.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = request.parcelas() != null ? request.parcelas() : 1;
        if (!formaPagamento.isParcelamentoValido(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
        if (!formaPagamento.isDisponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultado = formaPagamento.calcular(totalPedido, parcelas);

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.getPrazoDias(),
                seguro,
                resultado.ajuste(),
                resultado.totalFinal(),
                resultado.parcelas(),
                resultado.valorParcela(),
                nivelClube.calcularCredito(subtotal),
                nivelClube.temBrinde(subtotal)
        );
    }

    private List<ItemRequest> validarItens(List<ItemRequest> itens) {
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
        return itens;
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(new BigDecimal(item.quantidade())));
        }
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(new BigDecimal(item.quantidade())));
        }
        return peso;
    }

    private Cupom resolverCupomOpcional(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        try {
            return Cupom.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
    }

    private <E extends Enum<E>> E resolver(Class<E> tipo, String valor, String codigoErro) {
        if (valor == null || valor.isBlank()) {
            throw new CheckoutException(codigoErro);
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(codigoErro);
        }
    }
}
