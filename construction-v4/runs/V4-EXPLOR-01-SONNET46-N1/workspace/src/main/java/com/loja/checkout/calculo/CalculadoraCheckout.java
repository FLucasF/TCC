package com.loja.checkout.calculo;

import com.loja.checkout.CheckoutException;
import com.loja.checkout.api.CheckoutRequest;
import com.loja.checkout.api.CheckoutResponse;
import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.clube.NiveisClubeRegistro;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.CupomRegistro;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.entrega.ModalidadeEntregaRegistro;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.FormaPagamentoRegistro;
import com.loja.checkout.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CalculadoraCheckout {

    private final NiveisClubeRegistro niveisClubeRegistro;
    private final ModalidadeEntregaRegistro modalidadeEntregaRegistro;
    private final CupomRegistro cupomRegistro;
    private final FormaPagamentoRegistro formaPagamentoRegistro;

    public CalculadoraCheckout(
            NiveisClubeRegistro niveisClubeRegistro,
            ModalidadeEntregaRegistro modalidadeEntregaRegistro,
            CupomRegistro cupomRegistro,
            FormaPagamentoRegistro formaPagamentoRegistro) {
        this.niveisClubeRegistro = niveisClubeRegistro;
        this.modalidadeEntregaRegistro = modalidadeEntregaRegistro;
        this.cupomRegistro = cupomRegistro;
        this.formaPagamentoRegistro = formaPagamentoRegistro;
    }

    public CheckoutResponse calcular(CheckoutRequest req) {
        // 1. Valida carrinho
        List<ItemRequest> itens = req.itens();
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemRequest item : itens) {
            if (item.precoUnitario() == null || item.quantidade() == null || item.pesoKg() == null
                    || item.precoUnitario() <= 0 || item.quantidade() <= 0 || item.pesoKg() <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }

        BigDecimal subtotal = itens.stream()
                .map(i -> BigDecimal.valueOf(i.precoUnitario()).multiply(BigDecimal.valueOf(i.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal peso = itens.stream()
                .map(i -> BigDecimal.valueOf(i.pesoKg()).multiply(BigDecimal.valueOf(i.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Valida nível do clube
        NivelClube nivel = niveisClubeRegistro.buscar(req.nivelClube())
                .orElseThrow(() -> new CheckoutException("NIVEL_CLUBE_INVALIDO"));

        // 3. Valida região
        Regiao regiao = Regiao.fromCodigo(req.regiao())
                .orElseThrow(() -> new CheckoutException("REGIAO_INVALIDA"));

        // 4. Valida modalidade de entrega
        ModalidadeEntrega modalidade = modalidadeEntregaRegistro.buscar(req.modalidadeEntrega())
                .orElseThrow(() -> new CheckoutException("MODALIDADE_INVALIDA"));

        // 5. Verifica disponibilidade da modalidade
        if (!modalidade.disponivel(peso)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        // 6 e 7. Valida e verifica aplicabilidade do cupom
        Cupom cupom = null;
        if (req.cupom() != null && !req.cupom().isBlank()) {
            cupom = cupomRegistro.buscar(req.cupom())
                    .orElseThrow(() -> new CheckoutException("CUPOM_INVALIDO"));
            if (!cupom.aplicavel(subtotal, itens)) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
        }

        // 8. Valida forma de pagamento
        FormaPagamento pagamento = formaPagamentoRegistro.buscar(req.formaPagamento())
                .orElseThrow(() -> new CheckoutException("FORMA_PAGAMENTO_INVALIDA"));

        // 9. Valida número de parcelas
        int parcelas = req.parcelas() != null ? req.parcelas() : 1;
        if (!pagamento.parcelasValidas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        // Calcula os valores
        BigDecimal freteBase = modalidade.calcularFrete(peso).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal frete = nivel.freteGratis() ? BigDecimal.ZERO.setScale(2) : freteBase;

        BigDecimal descontoCupom = cupom != null
                ? cupom.calcularDesconto(subtotal, frete, itens).setScale(2, RoundingMode.HALF_EVEN)
                : BigDecimal.ZERO.setScale(2);

        BigDecimal seguro = regiao.taxa().multiply(subtotal).setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        // 10. Verifica disponibilidade da forma de pagamento para este total
        if (!pagamento.disponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultadoPag = pagamento.calcular(totalPedido, parcelas);

        BigDecimal ajuste = resultadoPag.totalFinal().subtract(totalPedido);
        BigDecimal credito = nivel.calcularCredito(subtotal).setScale(2, RoundingMode.HALF_EVEN);
        boolean brinde = nivel.brinde(subtotal);

        return new CheckoutResponse(
                subtotal.setScale(2, RoundingMode.HALF_EVEN),
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajuste,
                resultadoPag.totalFinal(),
                parcelas,
                resultadoPag.valorParcela(),
                credito,
                brinde
        );
    }
}
