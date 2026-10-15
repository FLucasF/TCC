package com.loja.checkout.aplicacao;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dominio.Carrinho;
import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.seguro.CalculadoraSeguro;
import com.loja.checkout.seguro.Regiao;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Monta o resumo da compra na ordem combinada: produtos, desconto do cupom,
 * frete, seguro, total do pedido e, por fim, o ajuste da forma de pagamento.
 */
@Service
public class CalculadoraResumo {

    private final Catalogo<ModalidadeEntrega> modalidades;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<NivelClube> niveis;
    private final Catalogo<FormaPagamento> formasPagamento;
    private final CalculadoraSeguro calculadoraSeguro;

    public CalculadoraResumo(Catalogo<ModalidadeEntrega> modalidades,
                             Catalogo<Cupom> cupons,
                             Catalogo<NivelClube> niveis,
                             Catalogo<FormaPagamento> formasPagamento,
                             CalculadoraSeguro calculadoraSeguro) {
        this.modalidades = modalidades;
        this.cupons = cupons;
        this.niveis = niveis;
        this.formasPagamento = formasPagamento;
        this.calculadoraSeguro = calculadoraSeguro;
    }

    public ResumoResponse calcular(ResumoRequest pedido) {
        if (pedido == null) {
            throw new CheckoutException(ErroCheckout.PEDIDO_INVALIDO);
        }

        // 1. Carrinho, 2. nivel do clube, 3. regiao, 4. e 5. entrega.
        Carrinho carrinho = lerCarrinho(pedido.itens());
        NivelClube nivel = niveis.exigir(pedido.nivelClube(), ErroCheckout.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = lerRegiao(pedido.regiao());
        ModalidadeEntrega modalidade =
                modalidades.exigir(pedido.modalidadeEntrega(), ErroCheckout.MODALIDADE_INVALIDA);
        if (!modalidade.atende(carrinho)) {
            throw new CheckoutException(ErroCheckout.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotalProdutos = carrinho.subtotalProdutos();
        // Quem tem frete gratis pelo clube ve o frete zerado no resumo.
        BigDecimal frete = nivel.temFreteGratis()
                ? Dinheiro.ZERO
                : Dinheiro.centavos(modalidade.calcularFrete(carrinho));

        // 6. e 7. cupom (um por pedido).
        BigDecimal descontoCupom = calcularDescontoCupom(pedido.cupom(), carrinho, frete);

        // 8. e 9. forma de pagamento e parcelamento.
        FormaPagamento formaPagamento =
                formasPagamento.exigir(pedido.formaPagamento(), ErroCheckout.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        if (!formaPagamento.permiteParcelas(parcelas)) {
            throw new CheckoutException(ErroCheckout.PARCELAMENTO_INVALIDO);
        }

        BigDecimal seguro = calculadoraSeguro.calcular(subtotalProdutos, regiao);
        BigDecimal totalPedido = Dinheiro.centavos(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        // 10. a forma de pagamento atende este pedido?
        if (!formaPagamento.disponivel(totalPedido)) {
            throw new CheckoutException(ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento pagamento = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal ajustePagamento = Dinheiro.centavos(pagamento.totalFinal().subtract(totalPedido));

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajustePagamento,
                Dinheiro.centavos(pagamento.totalFinal()),
                parcelas,
                Dinheiro.centavos(pagamento.valorParcela()),
                nivel.calcularCredito(subtotalProdutos),
                nivel.temBrinde(subtotalProdutos));
    }

    private BigDecimal calcularDescontoCupom(String codigo, Carrinho carrinho, BigDecimal frete) {
        if (codigo == null || codigo.isBlank()) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.exigir(codigo, ErroCheckout.CUPOM_INVALIDO);
        if (!cupom.aplicavel(carrinho, frete)) {
            throw new CheckoutException(ErroCheckout.CUPOM_NAO_APLICAVEL);
        }
        return Dinheiro.centavos(cupom.calcularDesconto(carrinho, frete));
    }

    private Carrinho lerCarrinho(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException(ErroCheckout.PEDIDO_INVALIDO);
        }
        List<Item> validados = new ArrayList<>(itens.size());
        for (ItemRequest item : itens) {
            if (item == null
                    || !positivo(item.precoUnitario())
                    || item.quantidade() == null || item.quantidade() <= 0
                    || !positivo(item.pesoKg())) {
                throw new CheckoutException(ErroCheckout.PEDIDO_INVALIDO);
            }
            validados.add(new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return new Carrinho(validados);
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private Regiao lerRegiao(String codigo) {
        if (codigo == null) {
            throw new CheckoutException(ErroCheckout.REGIAO_INVALIDA);
        }
        try {
            return Regiao.valueOf(codigo);
        } catch (IllegalArgumentException naoExiste) {
            throw new CheckoutException(ErroCheckout.REGIAO_INVALIDA);
        }
    }
}
