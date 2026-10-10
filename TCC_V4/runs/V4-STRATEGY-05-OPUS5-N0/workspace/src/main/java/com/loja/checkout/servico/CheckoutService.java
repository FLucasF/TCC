package com.loja.checkout.servico;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.comum.CheckoutException;
import com.loja.checkout.comum.CodigoErro;
import com.loja.checkout.comum.Dinheiro;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.dominio.clube.CatalogoNiveisClube;
import com.loja.checkout.dominio.clube.NivelClube;
import com.loja.checkout.dominio.cupom.CatalogoCupons;
import com.loja.checkout.dominio.cupom.ContextoCupom;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.entrega.CatalogoEntregas;
import com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import com.loja.checkout.dominio.pagamento.CatalogoFormasPagamento;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import com.loja.checkout.dominio.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Calcula o resumo da compra: produtos, desconto do cupom, frete, seguro,
 * total do pedido e o ajuste da forma de pagamento, nessa ordem.
 */
@Service
public class CheckoutService {

    private final CatalogoEntregas entregas;
    private final CatalogoCupons cupons;
    private final CatalogoNiveisClube niveisClube;
    private final CatalogoFormasPagamento formasPagamento;

    public CheckoutService(CatalogoEntregas entregas,
                           CatalogoCupons cupons,
                           CatalogoNiveisClube niveisClube,
                           CatalogoFormasPagamento formasPagamento) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.niveisClube = niveisClube;
        this.formasPagamento = formasPagamento;
    }

    public ResumoResponse calcularResumo(ResumoRequest requisicao) {
        if (requisicao == null) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }

        // 1. Carrinho
        List<ItemPedido> itens = lerItens(requisicao.itens());

        // 2. Nivel do clube
        NivelClube nivelClube = niveisClube.buscar(requisicao.nivelClube())
                .orElseThrow(() -> new CheckoutException(CodigoErro.NIVEL_CLUBE_INVALIDO));

        // 3. Regiao
        Regiao regiao = lerRegiao(requisicao.regiao());

        // 4. e 5. Modalidade de entrega
        ModalidadeEntrega modalidade = entregas.buscar(requisicao.modalidadeEntrega())
                .orElseThrow(() -> new CheckoutException(CodigoErro.MODALIDADE_INVALIDA));

        BigDecimal subtotalProdutos = Dinheiro.valor(itens.stream()
                .map(ItemPedido::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal pesoTotalKg = itens.stream()
                .map(ItemPedido::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!modalidade.atende(pesoTotalKg)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        // Quem e OURO nao paga frete nunca: o frete sai zerado no resumo.
        BigDecimal frete = nivelClube.temFreteGratis()
                ? Dinheiro.ZERO
                : Dinheiro.valor(modalidade.calcularFrete(pesoTotalKg));

        // 6. e 7. Cupom
        Cupom cupom = lerCupom(requisicao.cupom());
        BigDecimal descontoCupom = Dinheiro.ZERO;
        if (cupom != null) {
            ContextoCupom contexto = new ContextoCupom(itens, subtotalProdutos, frete);
            if (!cupom.aplicavel(contexto)) {
                throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
            }
            descontoCupom = Dinheiro.valor(cupom.calcularDesconto(contexto));
        }

        // 8. e 9. Forma de pagamento e parcelamento
        FormaPagamento formaPagamento = formasPagamento.buscar(requisicao.formaPagamento())
                .orElseThrow(() -> new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA));
        int parcelas = requisicao.parcelas() == null ? 1 : requisicao.parcelas();
        if (!formaPagamento.aceitaParcelas(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        BigDecimal seguro = Dinheiro.percentual(subtotalProdutos, regiao.percentualSeguro());
        BigDecimal totalPedido = Dinheiro.valor(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        // 10. A forma de pagamento atende um pedido desse valor?
        if (!formaPagamento.atende(totalPedido)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento pagamento = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal totalFinal = Dinheiro.valor(pagamento.totalFinal());
        BigDecimal ajustePagamento = Dinheiro.valor(totalFinal.subtract(totalPedido));

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoEntregaDias(),
                seguro,
                ajustePagamento,
                totalFinal,
                parcelas,
                Dinheiro.valor(pagamento.valorParcela()),
                Dinheiro.valor(nivelClube.calcularCredito(subtotalProdutos)),
                nivelClube.temBrinde(subtotalProdutos));
    }

    private List<ItemPedido> lerItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        return itens.stream().map(this::lerItem).toList();
    }

    private ItemPedido lerItem(ItemRequest item) {
        if (item == null
                || !positivo(item.precoUnitario())
                || item.quantidade() == null || item.quantidade() <= 0
                || !positivo(item.pesoKg())) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private Regiao lerRegiao(String codigo) {
        if (codigo != null) {
            for (Regiao regiao : Regiao.values()) {
                if (regiao.name().equals(codigo)) {
                    return regiao;
                }
            }
        }
        throw new CheckoutException(CodigoErro.REGIAO_INVALIDA);
    }

    /** Devolve null quando o cliente nao usou cupom. */
    private Cupom lerCupom(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        return cupons.buscar(codigo)
                .orElseThrow(() -> new CheckoutException(CodigoErro.CUPOM_INVALIDO));
    }
}
