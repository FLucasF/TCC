package com.loja.checkout;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.model.CheckoutRequest;
import com.loja.checkout.model.CheckoutResponse;
import com.loja.checkout.model.ItemCarrinho;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.regiao.Regiao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
public class CheckoutService {

    private final Map<String, ModalidadeEntrega> entregas;
    private final Map<String, Cupom> cupons;
    private final Map<String, NivelClube> niveis;
    private final Map<String, FormaPagamento> pagamentos;

    public CheckoutService(
            Map<String, ModalidadeEntrega> entregas,
            Map<String, Cupom> cupons,
            Map<String, NivelClube> niveis,
            Map<String, FormaPagamento> pagamentos) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.niveis = niveis;
        this.pagamentos = pagamentos;
    }

    public CheckoutResponse calcular(CheckoutRequest req) {
        int parcelas = req.parcelas() != null ? req.parcelas() : 1;

        validar(req, parcelas);

        List<ItemCarrinho> itens = req.itens();
        BigDecimal subtotalProdutos = calcularSubtotal(itens);
        BigDecimal pesoTotal = calcularPesoTotal(itens);

        NivelClube nivel = niveis.get(req.nivelClube());
        ModalidadeEntrega entrega = entregas.get(req.modalidadeEntrega());
        Regiao regiao = Regiao.de(req.regiao()).orElseThrow();
        FormaPagamento pagamento = pagamentos.get(req.formaPagamento());

        BigDecimal freteCalculado = entrega.calcularFrete(pesoTotal);
        BigDecimal frete = nivel.freteGratis() ? BigDecimal.ZERO.setScale(2) : freteCalculado;

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (req.cupom() != null) {
            Cupom cupom = cupons.get(req.cupom());
            descontoCupom = cupom.calcularDesconto(subtotalProdutos, frete, itens);
        }

        BigDecimal seguro = subtotalProdutos.multiply(regiao.getTaxa()).setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal totalPedido = subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro);

        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);

        BigDecimal credito = nivel.calcularCredito(subtotalProdutos);
        boolean brinde = nivel.temBrinde(subtotalProdutos);

        return new CheckoutResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                entrega.prazoEntregaDias(),
                seguro,
                resultado.ajuste(),
                resultado.totalFinal(),
                resultado.parcelas(),
                resultado.valorParcela(),
                credito,
                brinde);
    }

    private void validar(CheckoutRequest req, int parcelas) {
        if (req.itens() == null || req.itens().isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemCarrinho item : req.itens()) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }

        if (req.nivelClube() == null || !niveis.containsKey(req.nivelClube())) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }

        if (req.regiao() == null || Regiao.de(req.regiao()).isEmpty()) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }

        if (req.modalidadeEntrega() == null || !entregas.containsKey(req.modalidadeEntrega())) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        BigDecimal pesoTotal = calcularPesoTotal(req.itens());
        if (!entregas.get(req.modalidadeEntrega()).aceita(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        if (req.cupom() != null) {
            if (!cupons.containsKey(req.cupom())) {
                throw new CheckoutException("CUPOM_INVALIDO");
            }
            BigDecimal subtotal = calcularSubtotal(req.itens());
            if (!cupons.get(req.cupom()).aplicavel(subtotal, req.itens())) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
        }

        if (req.formaPagamento() == null || !pagamentos.containsKey(req.formaPagamento())) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        if (!pagamentos.get(req.formaPagamento()).aceitaParcelas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal subtotal = calcularSubtotal(req.itens());
        NivelClube nivel = niveis.get(req.nivelClube());
        ModalidadeEntrega entrega = entregas.get(req.modalidadeEntrega());
        BigDecimal freteCalculado = entrega.calcularFrete(pesoTotal);
        BigDecimal frete = nivel.freteGratis() ? BigDecimal.ZERO.setScale(2) : freteCalculado;

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (req.cupom() != null) {
            descontoCupom = cupons.get(req.cupom()).calcularDesconto(subtotal, frete, req.itens());
        }

        Regiao regiao = Regiao.de(req.regiao()).orElseThrow();
        BigDecimal seguro = subtotal.multiply(regiao.getTaxa()).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        if (!pagamentos.get(req.formaPagamento()).aceita(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private BigDecimal calcularSubtotal(List<ItemCarrinho> itens) {
        return itens.stream()
                .map(i -> i.precoUnitario()
                        .multiply(BigDecimal.valueOf(i.quantidade()))
                        .setScale(2, RoundingMode.HALF_EVEN))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularPesoTotal(List<ItemCarrinho> itens) {
        return itens.stream()
                .map(i -> i.pesoKg().multiply(BigDecimal.valueOf(i.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
