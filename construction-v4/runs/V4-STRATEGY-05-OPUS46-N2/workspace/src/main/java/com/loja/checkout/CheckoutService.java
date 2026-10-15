package com.loja.checkout;

import com.loja.checkout.CheckoutRequest.ItemRequest;
import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.regiao.Regiao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CheckoutService {

    public CheckoutResponse calcular(CheckoutRequest req) {
        validarItens(req);

        NivelClube nivel = NivelClube.buscar(req.nivelClube());
        if (nivel == null) throw new CheckoutException("NIVEL_CLUBE_INVALIDO");

        Regiao regiao = Regiao.buscar(req.regiao());
        if (regiao == null) throw new CheckoutException("REGIAO_INVALIDA");

        ModalidadeEntrega entrega = ModalidadeEntrega.buscar(req.modalidadeEntrega());
        if (entrega == null) throw new CheckoutException("MODALIDADE_INVALIDA");

        BigDecimal pesoTotal = calcularPeso(req);
        if (!entrega.disponivel(pesoTotal)) throw new CheckoutException("MODALIDADE_INDISPONIVEL");

        BigDecimal subtotal = calcularSubtotal(req);

        Cupom cupom = resolverCupom(req.cupom(), subtotal, req);

        FormaPagamento pagamento = FormaPagamento.buscar(req.formaPagamento());
        if (pagamento == null) throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");

        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!pagamento.parcelasValidas(parcelas)) throw new CheckoutException("PARCELAMENTO_INVALIDO");

        BigDecimal frete = nivel.freteGratis()
                ? BigDecimal.ZERO.setScale(2)
                : entrega.calcularFrete(pesoTotal);

        BigDecimal descontoCupom = cupom == null
                ? BigDecimal.ZERO.setScale(2)
                : cupom.calcularDesconto(subtotal, req.itens(), frete);

        BigDecimal seguro = subtotal.multiply(regiao.taxaSeguro())
                .setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal totalPedido = subtotal
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro);

        if (!pagamento.disponivel(totalPedido)) throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");

        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);
        BigDecimal ajuste = resultado.totalFinal().subtract(totalPedido);

        BigDecimal credito = subtotal.multiply(nivel.creditoPercentual())
                .setScale(2, RoundingMode.HALF_EVEN);

        boolean brinde = nivel.brinde(subtotal);

        return new CheckoutResponse(
                subtotal,
                descontoCupom,
                frete,
                entrega.prazoDias(),
                seguro,
                ajuste,
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela(),
                credito,
                brinde
        );
    }

    private void validarItens(CheckoutRequest req) {
        if (req.itens() == null || req.itens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemRequest item : req.itens()) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private BigDecimal calcularSubtotal(CheckoutRequest req) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : req.itens()) {
            subtotal = subtotal.add(
                    item.precoUnitario().multiply(new BigDecimal(item.quantidade()))
            );
        }
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularPeso(CheckoutRequest req) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : req.itens()) {
            peso = peso.add(item.pesoKg().multiply(new BigDecimal(item.quantidade())));
        }
        return peso;
    }

    private Cupom resolverCupom(String codigo, BigDecimal subtotal, CheckoutRequest req) {
        if (codigo == null || codigo.isBlank()) return null;

        Cupom cupom = Cupom.buscar(codigo);
        if (cupom == null) throw new CheckoutException("CUPOM_INVALIDO");
        if (!cupom.aplicavel(subtotal, req.itens())) throw new CheckoutException("CUPOM_NAO_APLICAVEL");

        return cupom;
    }
}
