package com.loja.checkout.service;

import com.loja.checkout.domain.*;
import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.exception.CheckoutException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
public class CheckoutService {

    public CheckoutResponse calcular(CheckoutRequest req) {
        // 1. Validate cart
        validarCarrinho(req.itens());

        // 2. Validate club level
        NivelClube nivel = parseEnum(NivelClube.class, req.nivelClube(), "NIVEL_CLUBE_INVALIDO");

        // 3. Validate region
        Regiao regiao = parseEnum(Regiao.class, req.regiao(), "REGIAO_INVALIDA");

        // 4. Validate delivery option (existence)
        ModalidadeEntrega modalidade = parseEnum(ModalidadeEntrega.class, req.modalidadeEntrega(), "MODALIDADE_INVALIDA");

        BigDecimal pesoTotal = req.itens().stream()
                .map(i -> i.pesoKg().multiply(BigDecimal.valueOf(i.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. Validate delivery availability
        if (!modalidade.disponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotalProdutos = req.itens().stream()
                .map(i -> i.precoUnitario().multiply(BigDecimal.valueOf(i.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_EVEN);

        // Frete needs to be known before evaluating coupon applicability.
        // OURO overrides to zero; otherwise the modality calculates it.
        BigDecimal frete = nivel.freteGratis()
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN)
                : modalidade.calcularFrete(pesoTotal);

        // 6. Validate coupon existence
        Optional<Cupom> cupomOpt = Optional.empty();
        if (req.cupom() != null) {
            cupomOpt = Cupom.buscar(req.cupom());
            if (cupomOpt.isEmpty()) {
                throw new CheckoutException("CUPOM_INVALIDO");
            }
        }

        // 7. Validate coupon applicability
        if (cupomOpt.isPresent() && !cupomOpt.get().aplicavel(subtotalProdutos, frete)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }

        // 8. Validate payment method
        FormaPagamento formaPagamento = parseEnum(FormaPagamento.class, req.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");

        int parcelas = req.parcelas() != null ? req.parcelas() : 1;

        // 9. Validate installment count
        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        List<Cupom.ItemCarrinho> itensCarrinho = req.itens().stream()
                .map(i -> new Cupom.ItemCarrinho(i.precoUnitario(), i.quantidade()))
                .toList();

        BigDecimal descontoCupom = cupomOpt
                .map(c -> c.calcularDesconto(subtotalProdutos, frete, itensCarrinho))
                .orElse(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN));

        BigDecimal seguro = subtotalProdutos.multiply(regiao.taxaSeguro())
                .setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal totalPedido = subtotalProdutos
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro);

        // 10. Validate payment availability
        if (!formaPagamento.disponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        BigDecimal ajustePagamento = formaPagamento.calcularAjuste(totalPedido, parcelas);
        BigDecimal totalFinal = totalPedido.add(ajustePagamento);
        BigDecimal valorParcela = totalFinal.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);

        BigDecimal creditoProximaCompra = subtotalProdutos.multiply(nivel.taxaCashback())
                .setScale(2, RoundingMode.HALF_EVEN);

        boolean brinde = nivel.limiarBrinde()
                .map(limiar -> subtotalProdutos.compareTo(limiar) > 0)
                .orElse(false);

        return new CheckoutResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajustePagamento,
                totalFinal,
                parcelas,
                valorParcela,
                creditoProximaCompra,
                brinde
        );
    }

    private void validarCarrinho(List<CheckoutRequest.Item> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (var item : itens) {
            if (item.precoUnitario() == null || item.quantidade() == null || item.pesoKg() == null) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
            if (item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() <= 0
                    || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private <T extends Enum<T>> T parseEnum(Class<T> type, String value, String errorCode) {
        if (value == null) throw new CheckoutException(errorCode);
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(errorCode);
        }
    }
}
