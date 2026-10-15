package com.loja.checkout.service;

import com.loja.checkout.domain.Cupom;
import com.loja.checkout.domain.CheckoutException;
import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.domain.Item;
import com.loja.checkout.domain.ModalidadeEntrega;
import com.loja.checkout.domain.Money;
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
 * Calcula o resumo da compra. As validações seguem exatamente a ordem combinada
 * com o atendimento: o primeiro problema encontrado é o que recusa o pedido.
 */
@Service
public class CheckoutService {

    public CheckoutResponse calcular(CheckoutRequest req) {
        // 1. Itens válidos.
        List<Item> itens = validarItens(req == null ? null : req.itens());
        BigDecimal subtotal = somaProdutos(itens);
        BigDecimal pesoKg = somaPeso(itens);

        // 2. Nível do clube.
        NivelClube nivel = parse(NivelClube.class, req.nivelClube(), "NIVEL_CLUBE_INVALIDO");

        // 3. Região.
        Regiao regiao = parse(Regiao.class, req.regiao(), "REGIAO_INVALIDA");

        // 4. Modalidade de entrega.
        ModalidadeEntrega modalidade = parse(ModalidadeEntrega.class, req.modalidadeEntrega(), "MODALIDADE_INVALIDA");

        // 5. Modalidade atende o pedido?
        if (!modalidade.atende(pesoKg)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        // Frete exibido: valor da modalidade, zerado quando o nível não paga frete (OURO).
        BigDecimal frete = nivel.naoPagaFrete() ? Money.cents(BigDecimal.ZERO) : modalidade.frete(pesoKg);

        // 6. Cupom existe? (só valida quando o cliente informou um.)
        Cupom cupom = resolverCupom(req.cupom());

        // 7. Cupom aplicável ao pedido?
        BigDecimal descontoCupom = Money.cents(BigDecimal.ZERO);
        if (cupom != null) {
            Cupom.Contexto ctx = new Cupom.Contexto(subtotal, frete, itens);
            if (!cupom.aplicavel(ctx)) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = cupom.desconto(ctx);
        }

        // 8. Forma de pagamento.
        FormaPagamento forma = parse(FormaPagamento.class, req.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");

        // 9. Parcelas permitidas para a forma de pagamento.
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!forma.parcelasPermitidas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        // Seguro e total do pedido.
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = Money.cents(
                subtotal.subtract(descontoCupom).add(frete).add(seguro));

        // 10. Forma de pagamento atende o pedido? (ex.: boleto acima de R$ 1.000,00)
        if (!forma.disponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento pagamento = forma.calcular(totalPedido, parcelas);

        return new CheckoutResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                pagamento.ajuste(),
                pagamento.totalFinal(),
                pagamento.parcelas(),
                pagamento.valorParcela(),
                nivel.credito(subtotal),
                nivel.temBrinde(subtotal));
    }

    private List<Item> validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        List<Item> validos = new ArrayList<>(itens.size());
        for (ItemRequest it : itens) {
            if (it == null
                    || it.precoUnitario() == null || it.precoUnitario().signum() <= 0
                    || it.quantidade() == null || it.quantidade() <= 0
                    || it.pesoKg() == null || it.pesoKg().signum() <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
            validos.add(new Item(it.nome(), it.precoUnitario(), it.quantidade(), it.pesoKg()));
        }
        return validos;
    }

    private BigDecimal somaProdutos(List<Item> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (Item it : itens) {
            total = total.add(it.totalProdutos());
        }
        return Money.cents(total);
    }

    private BigDecimal somaPeso(List<Item> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (Item it : itens) {
            total = total.add(it.pesoTotal());
        }
        return total;
    }

    private Cupom resolverCupom(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        try {
            return Cupom.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
    }

    private <E extends Enum<E>> E parse(Class<E> tipo, String valor, String codigoErro) {
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
