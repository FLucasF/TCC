package com.loja.checkout.servico;

import com.loja.checkout.dominio.*;
import com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import com.loja.checkout.dominio.entrega.RegistroEntregas;
import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.excecao.CheckoutException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CheckoutService {

    private final RegistroEntregas registroEntregas;

    public CheckoutService(RegistroEntregas registroEntregas) {
        this.registroEntregas = registroEntregas;
    }

    public CheckoutResponse calcular(CheckoutRequest request) {
        validarItens(request.itens());

        NivelClube nivel = resolverEnum(NivelClube.class, request.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = resolverEnum(Regiao.class, request.regiao(), "REGIAO_INVALIDA");

        BigDecimal subtotal = calcularSubtotal(request.itens());
        BigDecimal pesoTotal = calcularPeso(request.itens());

        ModalidadeEntrega entrega = registroEntregas.buscar(request.modalidadeEntrega())
                .orElseThrow(() -> new CheckoutException("MODALIDADE_INVALIDA"));

        if (!entrega.disponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal frete = entrega.calcularFrete(pesoTotal);
        int prazo = entrega.prazoDias();

        if (nivel.freteGratis()) {
            frete = new BigDecimal("0.00");
        }

        BigDecimal desconto = new BigDecimal("0.00");
        if (request.cupom() != null && !request.cupom().isEmpty()) {
            Cupom cupom = resolverEnum(Cupom.class, request.cupom(), "CUPOM_INVALIDO");
            if (!cupom.aplicavel(request.itens(), subtotal)) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
            desconto = cupom.calcularDesconto(request.itens(), subtotal, frete);
        }

        BigDecimal seguro = regiao.calcularSeguro(subtotal);

        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);

        FormaPagamento pagamento = resolverEnum(FormaPagamento.class, request.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");

        int parcelas = request.parcelas() != null ? request.parcelas() : 1;

        if (!pagamento.parcelasValidas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        if (!pagamento.disponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);

        BigDecimal ajuste = resultado.totalFinal().subtract(totalPedido).setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal credito = nivel.calcularCredito(subtotal);
        boolean brinde = nivel.temBrinde(subtotal);

        return new CheckoutResponse(
                subtotal, desconto, frete, prazo, seguro,
                ajuste, resultado.totalFinal(), parcelas, resultado.valorParcela(),
                credito, brinde
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

    private <E extends Enum<E>> E resolverEnum(Class<E> tipo, String valor, String codigoErro) {
        if (valor == null || valor.isEmpty()) {
            throw new CheckoutException(codigoErro);
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(codigoErro);
        }
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(new BigDecimal(item.quantidade())));
        }
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularPeso(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(new BigDecimal(item.quantidade())));
        }
        return peso;
    }
}
