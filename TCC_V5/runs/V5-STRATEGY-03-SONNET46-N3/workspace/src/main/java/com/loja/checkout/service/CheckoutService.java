package com.loja.checkout.service;

import com.loja.checkout.domain.Regiao;
import com.loja.checkout.domain.clube.NivelClubeRegistro;
import com.loja.checkout.domain.clube.NivelClubeStrategy;
import com.loja.checkout.domain.cupom.CupomRegistro;
import com.loja.checkout.domain.cupom.CupomStrategy;
import com.loja.checkout.domain.entrega.ModalidadeEntregaRegistro;
import com.loja.checkout.domain.entrega.ModalidadeEntregaStrategy;
import com.loja.checkout.domain.pagamento.FormaPagamentoRegistro;
import com.loja.checkout.domain.pagamento.FormaPagamentoStrategy;
import com.loja.checkout.domain.pagamento.ResultadoPagamento;
import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemDto;
import com.loja.checkout.exception.CheckoutException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CheckoutService {

    private final ModalidadeEntregaRegistro entregaRegistro;
    private final CupomRegistro cupomRegistro;
    private final NivelClubeRegistro clubeRegistro;
    private final FormaPagamentoRegistro pagamentoRegistro;

    public CheckoutService(ModalidadeEntregaRegistro entregaRegistro,
                           CupomRegistro cupomRegistro,
                           NivelClubeRegistro clubeRegistro,
                           FormaPagamentoRegistro pagamentoRegistro) {
        this.entregaRegistro = entregaRegistro;
        this.cupomRegistro = cupomRegistro;
        this.clubeRegistro = clubeRegistro;
        this.pagamentoRegistro = pagamentoRegistro;
    }

    public CheckoutResponse calcular(CheckoutRequest req) {
        // 1. PEDIDO_INVALIDO
        if (req.itens() == null || req.itens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemDto item : req.itens()) {
            if (item.precoUnitario() == null || item.precoUnitario().signum() <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().signum() <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }

        // 2. NIVEL_CLUBE_INVALIDO
        NivelClubeStrategy clube = clubeRegistro.buscar(req.nivelClube());
        if (clube == null) throw new CheckoutException("NIVEL_CLUBE_INVALIDO");

        // 3. REGIAO_INVALIDA
        Regiao regiao = Regiao.buscar(req.regiao());
        if (regiao == null) throw new CheckoutException("REGIAO_INVALIDA");

        // 4. MODALIDADE_INVALIDA
        ModalidadeEntregaStrategy modalidade = entregaRegistro.buscar(req.modalidadeEntrega());
        if (modalidade == null) throw new CheckoutException("MODALIDADE_INVALIDA");

        // 5. MODALIDADE_INDISPONIVEL
        BigDecimal pesoTotal = calcularPesoTotal(req.itens());
        if (!modalidade.atende(pesoTotal)) throw new CheckoutException("MODALIDADE_INDISPONIVEL");

        // subtotal necessário para validar cupom
        BigDecimal subtotal = calcularSubtotal(req.itens());

        // 6. CUPOM_INVALIDO / 7. CUPOM_NAO_APLICAVEL
        CupomStrategy cupom = null;
        if (req.cupom() != null && !req.cupom().isBlank()) {
            cupom = cupomRegistro.buscar(req.cupom());
            if (cupom == null) throw new CheckoutException("CUPOM_INVALIDO");
            if (!cupom.aplicavel(subtotal)) throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }

        // 8. FORMA_PAGAMENTO_INVALIDA
        FormaPagamentoStrategy pagamento = pagamentoRegistro.buscar(req.formaPagamento());
        if (pagamento == null) throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");

        // 9. PARCELAMENTO_INVALIDO
        int parcelas = req.parcelas() != null ? req.parcelas() : 1;
        if (!pagamento.aceitaParcelas(parcelas)) throw new CheckoutException("PARCELAMENTO_INVALIDO");

        // Cálculo: frete
        BigDecimal frete = clube.isentoFrete() ? new BigDecimal("0.00") : modalidade.calcularFrete(pesoTotal);

        // Cálculo: desconto do cupom
        BigDecimal descontoCupom = cupom != null
                ? cupom.calcularDesconto(subtotal, frete, req.itens())
                : new BigDecimal("0.00");

        // Cálculo: seguro (sobre subtotal bruto)
        BigDecimal seguro = subtotal.multiply(regiao.getTaxa()).setScale(2, RoundingMode.HALF_EVEN);

        // Cálculo: total do pedido (antes do ajuste de pagamento)
        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        // 10. FORMA_PAGAMENTO_INDISPONIVEL
        if (!pagamento.disponivelParaTotal(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        // Cálculo: resultado do pagamento
        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);

        BigDecimal ajuste = resultado.totalFinal().subtract(totalPedido).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal credito = clube.calcularCredito(subtotal);
        boolean brinde = clube.contemBrinde(subtotal);

        return new CheckoutResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoEntregaDias(),
                seguro,
                ajuste,
                resultado.totalFinal(),
                resultado.parcelas(),
                resultado.valorParcela(),
                credito,
                brinde);
    }

    private BigDecimal calcularSubtotal(List<ItemDto> itens) {
        return itens.stream()
                .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularPesoTotal(List<ItemDto> itens) {
        return itens.stream()
                .map(item -> item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
