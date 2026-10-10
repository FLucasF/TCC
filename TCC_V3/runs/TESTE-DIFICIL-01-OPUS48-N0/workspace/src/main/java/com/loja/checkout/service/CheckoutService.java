package com.loja.checkout.service;

import com.loja.checkout.domain.CheckoutException;
import com.loja.checkout.domain.Cupom;
import com.loja.checkout.domain.Dinheiro;
import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.domain.ItemPedido;
import com.loja.checkout.domain.ModalidadeEntrega;
import com.loja.checkout.domain.NivelClube;
import com.loja.checkout.domain.Regiao;
import com.loja.checkout.domain.ResultadoPagamento;
import com.loja.checkout.web.CheckoutRequest;
import com.loja.checkout.web.CheckoutResponse;
import com.loja.checkout.web.ItemRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Calcula o resumo da compra. As validacoes seguem exatamente a ordem definida
 * pela loja e devolvem o primeiro problema encontrado; nao havendo problema,
 * calcula cada parte do resumo na ordem combinada.
 */
@Service
public class CheckoutService {

    public CheckoutResponse calcular(CheckoutRequest req) {
        // 1. Itens do carrinho.
        List<ItemPedido> itens = validarItens(req == null ? null : req.itens());
        BigDecimal subtotalProdutos = Dinheiro.centavos(somarProdutos(itens));
        BigDecimal pesoTotal = somarPeso(itens);

        // 2. Nivel do clube.
        NivelClube nivel = parseEnum(NivelClube.class, req.nivelClube(), "NIVEL_CLUBE_INVALIDO");

        // 3. Regiao.
        Regiao regiao = parseEnum(Regiao.class, req.regiao(), "REGIAO_INVALIDA");

        // 4. Modalidade de entrega existe.
        ModalidadeEntrega modalidade = parseEnum(ModalidadeEntrega.class, req.modalidadeEntrega(), "MODALIDADE_INVALIDA");

        // 5. Modalidade atende o pedido.
        if (!modalidade.atende(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        // Frete: calculado pela modalidade e zerado quando o nivel nao paga frete.
        BigDecimal frete = Dinheiro.centavos(modalidade.frete(pesoTotal));
        if (nivel.freteGratis()) {
            frete = Dinheiro.ZERO;
        }

        // 6. Cupom existe (quando informado).
        Cupom cupom = parseCupom(req.cupom());

        // 7. Cupom se aplica ao pedido.
        Cupom.Contexto contextoCupom = new Cupom.Contexto(subtotalProdutos, frete, itens);
        if (cupom != null && !cupom.aplicavel(contextoCupom)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }

        // 8. Forma de pagamento existe.
        FormaPagamento formaPagamento = parseEnum(FormaPagamento.class, req.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");

        // 9. Numero de parcelas permitido.
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!formaPagamento.parcelasPermitidas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        // Partes do resumo que dependem apenas do que ja foi validado.
        BigDecimal descontoCupom = cupom == null ? Dinheiro.ZERO : cupom.desconto(contextoCupom);
        BigDecimal seguro = modalidade.temSeguro()
                ? Dinheiro.centavos(subtotalProdutos.multiply(regiao.percentualSeguro()))
                : Dinheiro.ZERO;

        // Total do pedido = produtos - desconto + frete + seguro.
        BigDecimal totalPedido = Dinheiro.centavos(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        // 10. Forma de pagamento atende o pedido (ex.: boleto ate R$ 1.000,00).
        if (!formaPagamento.atende(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        // Ajuste da forma de pagamento sobre o total.
        ResultadoPagamento pagamento = formaPagamento.calcular(totalPedido, parcelas, nivel);
        BigDecimal ajustePagamento = Dinheiro.centavos(pagamento.totalFinal().subtract(totalPedido));

        // Vantagens do clube.
        BigDecimal credito = Dinheiro.centavos(subtotalProdutos.multiply(nivel.percentualCredito()));
        boolean brinde = nivel.temBrinde(subtotalProdutos);

        int prazo = modalidade.prazoDias() + formaPagamento.diasAdicionaisPrazo();

        return new CheckoutResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                prazo,
                seguro,
                ajustePagamento,
                pagamento.totalFinal(),
                pagamento.parcelas(),
                pagamento.valorParcela(),
                credito,
                brinde);
    }

    private List<ItemPedido> validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        List<ItemPedido> validados = new ArrayList<>();
        for (ItemRequest item : itens) {
            if (item == null
                    || !positivo(item.precoUnitario())
                    || item.quantidade() == null || item.quantidade() <= 0
                    || !positivo(item.pesoKg())) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
            validados.add(new ItemPedido(item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return validados;
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal somarProdutos(List<ItemPedido> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            total = total.add(item.totalProdutos());
        }
        return total;
    }

    private BigDecimal somarPeso(List<ItemPedido> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            total = total.add(item.pesoTotal());
        }
        return total;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> tipo, String valor, String codigoErro) {
        if (valor == null) {
            throw new CheckoutException(codigoErro);
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(codigoErro);
        }
    }

    private Cupom parseCupom(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        try {
            return Cupom.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
    }
}
