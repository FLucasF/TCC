package com.loja.checkout.domain;

import com.loja.checkout.domain.clube.NivelClube;
import com.loja.checkout.domain.clube.RegistroClube;
import com.loja.checkout.domain.cupom.Cupom;
import com.loja.checkout.domain.cupom.RegistroCupom;
import com.loja.checkout.domain.entrega.ModalidadeEntrega;
import com.loja.checkout.domain.entrega.RegistroEntrega;
import com.loja.checkout.domain.pagamento.FormaPagamento;
import com.loja.checkout.domain.pagamento.RegistroPagamento;
import com.loja.checkout.domain.pagamento.ResultadoPagamento;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.PedidoRequest;
import com.loja.checkout.web.ResumoResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CheckoutService {

    private final RegistroEntrega registroEntrega;
    private final RegistroCupom registroCupom;
    private final RegistroClube registroClube;
    private final RegistroPagamento registroPagamento;

    public CheckoutService(RegistroEntrega registroEntrega,
                           RegistroCupom registroCupom,
                           RegistroClube registroClube,
                           RegistroPagamento registroPagamento) {
        this.registroEntrega = registroEntrega;
        this.registroCupom = registroCupom;
        this.registroClube = registroClube;
        this.registroPagamento = registroPagamento;
    }

    public ResumoResponse calcular(PedidoRequest pedido) {
        validarItens(pedido.itens());

        NivelClube clube = registroClube.buscar(pedido.nivelClube())
                .orElseThrow(() -> new CheckoutException("NIVEL_CLUBE_INVALIDO"));

        Regiao regiao = Regiao.buscar(pedido.regiao())
                .orElseThrow(() -> new CheckoutException("REGIAO_INVALIDA"));

        ModalidadeEntrega entrega = registroEntrega.buscar(pedido.modalidadeEntrega())
                .orElseThrow(() -> new CheckoutException("MODALIDADE_INVALIDA"));

        BigDecimal pesoTotal = calcularPeso(pedido.itens());

        if (!entrega.disponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotal = calcularSubtotal(pedido.itens());

        BigDecimal freteCalculado = entrega.calcularFrete(pesoTotal);
        BigDecimal frete = clube.freteIsento() ? new BigDecimal("0.00") : freteCalculado;

        BigDecimal descontoCupom = new BigDecimal("0.00");
        if (pedido.cupom() != null) {
            Cupom cupom = registroCupom.buscar(pedido.cupom())
                    .orElseThrow(() -> new CheckoutException("CUPOM_INVALIDO"));
            if (!cupom.aplicavel(subtotal, pedido.itens())) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = cupom.calcularDesconto(subtotal, pedido.itens(), frete);
        }

        BigDecimal seguro = regiao.taxa().multiply(subtotal).setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal totalBase = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        int parcelas = pedido.parcelas() != null ? pedido.parcelas() : 1;

        FormaPagamento pagamento = registroPagamento.buscar(pedido.formaPagamento())
                .orElseThrow(() -> new CheckoutException("FORMA_PAGAMENTO_INVALIDA"));

        if (!pagamento.aceitaParcelas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        if (!pagamento.disponivel(totalBase)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultado = pagamento.calcular(totalBase, parcelas);

        BigDecimal credito = clube.calcularCredito(subtotal);
        boolean brinde = clube.brinde(subtotal);

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                entrega.prazoDias(),
                seguro,
                resultado.ajuste(),
                resultado.totalFinal(),
                parcelas,
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

    private BigDecimal calcularPeso(List<ItemRequest> itens) {
        return itens.stream()
                .map(i -> i.pesoKg().multiply(BigDecimal.valueOf(i.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        return itens.stream()
                .map(i -> i.precoUnitario()
                        .multiply(BigDecimal.valueOf(i.quantidade()))
                        .setScale(2, RoundingMode.HALF_EVEN))
                .reduce(new BigDecimal("0.00"), BigDecimal::add);
    }
}
