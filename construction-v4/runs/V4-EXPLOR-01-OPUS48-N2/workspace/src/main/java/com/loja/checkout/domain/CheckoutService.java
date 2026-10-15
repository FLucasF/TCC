package com.loja.checkout.domain;

import com.loja.checkout.api.CheckoutRequest;
import com.loja.checkout.api.CheckoutResponse;
import com.loja.checkout.api.ItemCarrinho;
import com.loja.checkout.domain.clube.NivelClube;
import com.loja.checkout.domain.clube.NiveisClube;
import com.loja.checkout.domain.cupom.Cupom;
import com.loja.checkout.domain.cupom.CupomContexto;
import com.loja.checkout.domain.cupom.Cupons;
import com.loja.checkout.domain.entrega.ModalidadeEntrega;
import com.loja.checkout.domain.entrega.ModalidadesEntrega;
import com.loja.checkout.domain.pagamento.FormaPagamento;
import com.loja.checkout.domain.pagamento.FormasPagamento;
import com.loja.checkout.domain.pagamento.PagamentoResultado;
import com.loja.checkout.domain.regiao.Regiao;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Monta o resumo da compra. A sequência de etapas é a mesma para todo pedido;
 * o que muda de caso para caso mora nas modalidades, cupons, níveis e formas de
 * pagamento. Cada conferência é feita na ordem definida e recusa o pedido no
 * primeiro problema.
 */
@Service
public class CheckoutService {

    private final ModalidadesEntrega modalidades;
    private final Cupons cupons;
    private final NiveisClube niveis;
    private final FormasPagamento formas;

    public CheckoutService(ModalidadesEntrega modalidades, Cupons cupons,
                           NiveisClube niveis, FormasPagamento formas) {
        this.modalidades = modalidades;
        this.cupons = cupons;
        this.niveis = niveis;
        this.formas = formas;
    }

    public CheckoutResponse calcular(CheckoutRequest req) {
        List<ItemCarrinho> itens = exigirItensValidos(req.itens());

        NivelClube nivel = niveis.buscar(req.nivelClube())
                .orElseThrow(() -> new CheckoutException(CodigoErro.NIVEL_CLUBE_INVALIDO));
        Regiao regiao = Regiao.buscar(req.regiao())
                .orElseThrow(() -> new CheckoutException(CodigoErro.REGIAO_INVALIDA));
        ModalidadeEntrega modalidade = modalidades.buscar(req.modalidadeEntrega())
                .orElseThrow(() -> new CheckoutException(CodigoErro.MODALIDADE_INVALIDA));

        BigDecimal peso = pesoTotal(itens);
        if (!modalidade.atende(peso)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotal = Dinheiro.centavos(subtotalProdutos(itens));
        BigDecimal freteCobrado = Dinheiro.centavos(modalidade.frete(peso));
        BigDecimal frete = nivel.freteGratis() ? Dinheiro.ZERO : freteCobrado;

        BigDecimal desconto = descontoCupom(req.cupom(), new CupomContexto(subtotal, frete, itens));
        BigDecimal seguro = Dinheiro.centavos(subtotal.multiply(regiao.percentualSeguro()));
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);

        FormaPagamento forma = formas.buscar(req.formaPagamento())
                .orElseThrow(() -> new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA));
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!forma.parcelasPermitidas(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!forma.atende(totalPedido)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        PagamentoResultado pagamento = forma.calcular(totalPedido, parcelas);
        BigDecimal ajuste = Dinheiro.centavos(pagamento.totalFinal().subtract(totalPedido));

        return new CheckoutResponse(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajuste,
                Dinheiro.centavos(pagamento.totalFinal()),
                parcelas,
                Dinheiro.centavos(pagamento.valorParcela()),
                nivel.credito(subtotal),
                nivel.brinde(subtotal));
    }

    private BigDecimal descontoCupom(String codigo, CupomContexto ctx) {
        if (codigo == null || codigo.isBlank()) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(codigo)
                .orElseThrow(() -> new CheckoutException(CodigoErro.CUPOM_INVALIDO));
        if (!cupom.aplicavel(ctx)) {
            throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return Dinheiro.centavos(cupom.desconto(ctx));
    }

    private List<ItemCarrinho> exigirItensValidos(List<ItemCarrinho> itens) {
        if (itens == null || itens.isEmpty() || itens.stream().anyMatch(this::itemInvalido)) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        return itens;
    }

    private boolean itemInvalido(ItemCarrinho item) {
        return item == null
                || item.precoUnitario() == null || naoPositivo(item.precoUnitario())
                || item.quantidade() == null || item.quantidade() <= 0
                || item.pesoKg() == null || naoPositivo(item.pesoKg());
    }

    private boolean naoPositivo(BigDecimal valor) {
        return valor.compareTo(BigDecimal.ZERO) <= 0;
    }

    private BigDecimal subtotalProdutos(List<ItemCarrinho> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return total;
    }

    private BigDecimal pesoTotal(List<ItemCarrinho> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            total = total.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return total;
    }
}
