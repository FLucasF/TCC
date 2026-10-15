package com.loja.checkout.servico;

import com.loja.checkout.api.CheckoutRequest;
import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.CupomContexto;
import com.loja.checkout.cupom.CupomRegistry;
import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.NivelClube;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.entrega.ModalidadeEntregaRegistry;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.FormaPagamentoRegistry;
import com.loja.checkout.pagamento.ResultadoPagamento;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Calcula o resumo da compra na ordem combinada com o financeiro:
 * produtos → desconto do cupom → frete → seguro → total do pedido → ajuste do pagamento.
 *
 * <p>As verificações de recusa seguem exatamente a ordem da tabela de erros:
 * o primeiro problema encontrado interrompe o cálculo.
 */
@Service
public class ResumoService {

    private final ModalidadeEntregaRegistry modalidades;
    private final CupomRegistry cupons;
    private final FormaPagamentoRegistry formasPagamento;

    public ResumoService(ModalidadeEntregaRegistry modalidades, CupomRegistry cupons,
                         FormaPagamentoRegistry formasPagamento) {
        this.modalidades = modalidades;
        this.cupons = cupons;
        this.formasPagamento = formasPagamento;
    }

    public ResumoResponse calcular(CheckoutRequest req) {
        // 1. Itens válidos
        List<ItemPedido> itens = validarItens(req);

        // 2. Nível do clube
        NivelClube clube = parseEnum(NivelClube.class, req.nivelClube(), ErroCheckout.NIVEL_CLUBE_INVALIDO);

        // 3. Região
        Regiao regiao = parseEnum(Regiao.class, req.regiao(), ErroCheckout.REGIAO_INVALIDA);

        // 4. Modalidade de entrega existe
        ModalidadeEntrega modalidade = modalidades.buscar(req.modalidadeEntrega())
                .orElseThrow(() -> new CheckoutException(ErroCheckout.MODALIDADE_INVALIDA));

        // 5. Modalidade atende o pedido (ex.: motoboy acima de 5 kg)
        BigDecimal pesoKg = pesoTotal(itens);
        if (!modalidade.atende(pesoKg)) {
            throw new CheckoutException(ErroCheckout.MODALIDADE_INDISPONIVEL);
        }

        // Valores base
        BigDecimal subtotal = subtotalProdutos(itens);
        BigDecimal frete = clube.isFreteGratisSempre()
                ? Dinheiro.ZERO
                : modalidade.custoFrete(pesoKg);

        // 6-7. Cupom
        BigDecimal descontoCupom = resolverDescontoCupom(req.cupom(), itens, subtotal, frete);

        // 8. Forma de pagamento existe
        FormaPagamento forma = formasPagamento.buscar(req.formaPagamento())
                .orElseThrow(() -> new CheckoutException(ErroCheckout.FORMA_PAGAMENTO_INVALIDA));

        // 9. Parcelas permitidas
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!forma.parcelasPermitidas(parcelas)) {
            throw new CheckoutException(ErroCheckout.PARCELAMENTO_INVALIDO);
        }

        // Seguro e total do pedido
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = Dinheiro.centavos(
                subtotal.subtract(descontoCupom).add(frete).add(seguro));

        // 10. Forma de pagamento atende o pedido (ex.: boleto acima de R$ 1.000,00)
        if (!forma.atende(totalPedido)) {
            throw new CheckoutException(ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        // Ajuste do pagamento
        ResultadoPagamento pagamento = forma.calcular(totalPedido, parcelas);

        // Vantagens do clube
        BigDecimal credito = clube.creditoProximaCompra(subtotal);
        boolean brinde = clube.temBrinde(subtotal);

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                pagamento.ajustePagamento(),
                pagamento.totalFinal(),
                pagamento.parcelas(),
                pagamento.valorParcela(),
                credito,
                brinde);
    }

    private List<ItemPedido> validarItens(CheckoutRequest req) {
        if (req == null || req.itens() == null || req.itens().isEmpty()) {
            throw new CheckoutException(ErroCheckout.PEDIDO_INVALIDO);
        }
        List<ItemPedido> itens = new ArrayList<>();
        for (ItemRequest item : req.itens()) {
            if (item == null
                    || !positivo(item.precoUnitario())
                    || item.quantidade() == null || item.quantidade() <= 0
                    || !positivo(item.pesoKg())) {
                throw new CheckoutException(ErroCheckout.PEDIDO_INVALIDO);
            }
            itens.add(new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return itens;
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal subtotalProdutos(List<ItemPedido> itens) {
        BigDecimal soma = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            soma = soma.add(item.totalItem());
        }
        return Dinheiro.centavos(soma);
    }

    private BigDecimal pesoTotal(List<ItemPedido> itens) {
        BigDecimal soma = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            soma = soma.add(item.pesoTotal());
        }
        return soma;
    }

    private BigDecimal resolverDescontoCupom(String codigo, List<ItemPedido> itens,
                                             BigDecimal subtotal, BigDecimal frete) {
        if (codigo == null) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(codigo)
                .orElseThrow(() -> new CheckoutException(ErroCheckout.CUPOM_INVALIDO));
        CupomContexto contexto = new CupomContexto(itens, subtotal, frete);
        if (!cupom.aplicavel(contexto)) {
            throw new CheckoutException(ErroCheckout.CUPOM_NAO_APLICAVEL);
        }
        return cupom.desconto(contexto);
    }

    private <E extends Enum<E>> E parseEnum(Class<E> tipo, String valor, ErroCheckout erro) {
        if (valor == null) {
            throw new CheckoutException(erro);
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(erro);
        }
    }
}
