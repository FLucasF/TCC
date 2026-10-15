package com.loja.checkout.servico;

import com.loja.checkout.api.dto.ItemRequest;
import com.loja.checkout.api.dto.ResumoCompraRequest;
import com.loja.checkout.api.dto.ResumoCompraResponse;
import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.ItemCarrinho;
import com.loja.checkout.dominio.Moeda;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.dominio.clube.NivelClube;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.cupom.DadosCupom;
import com.loja.checkout.dominio.entrega.DadosEntrega;
import com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import com.loja.checkout.dominio.pagamento.ResultadoPagamento;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Calcula o resumo da compra.
 *
 * <p>Ordem do calculo: produtos, desconto do cupom, frete, seguro,
 * total do pedido e, por fim, o ajuste da forma de pagamento.
 */
@Service
public class CheckoutService {

    private final Catalogo<ModalidadeEntrega> entregas;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<NivelClube> niveisClube;
    private final Catalogo<FormaPagamento> formasPagamento;

    public CheckoutService(Catalogo<ModalidadeEntrega> entregas,
                           Catalogo<Cupom> cupons,
                           Catalogo<NivelClube> niveisClube,
                           Catalogo<FormaPagamento> formasPagamento) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.niveisClube = niveisClube;
        this.formasPagamento = formasPagamento;
    }

    public ResumoCompraResponse calcular(ResumoCompraRequest pedido) {
        // 1. Carrinho
        List<ItemCarrinho> itens = lerItens(pedido);
        BigDecimal subtotalProdutos = somarProdutos(itens);
        BigDecimal pesoKg = somarPeso(itens);

        // 2. e 3. Quem e o cliente
        NivelClube nivelClube = niveisClube.buscar(pedido.nivelClube())
                .orElseThrow(() -> new CheckoutException(CodigoErro.NIVEL_CLUBE_INVALIDO));
        Regiao regiao = Regiao.porCodigo(pedido.regiao())
                .orElseThrow(() -> new CheckoutException(CodigoErro.REGIAO_INVALIDA));

        // 4. e 5. Entrega
        ModalidadeEntrega modalidade = entregas.buscar(pedido.modalidadeEntrega())
                .orElseThrow(() -> new CheckoutException(CodigoErro.MODALIDADE_INVALIDA));
        DadosEntrega dadosEntrega = new DadosEntrega(pesoKg, subtotalProdutos);
        if (!modalidade.atende(dadosEntrega)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
        BigDecimal frete = nivelClube.freteGratis()
                ? Moeda.ZERO
                : Moeda.emCentavos(modalidade.calcularFrete(dadosEntrega));

        // 6. e 7. Cupom (opcional)
        BigDecimal descontoCupom = calcularDescontoCupom(pedido.cupom(), itens, subtotalProdutos, frete);

        // Seguro, total do pedido
        BigDecimal seguro = regiao.calcularSeguro(subtotalProdutos);
        BigDecimal totalPedido = Moeda.emCentavos(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        // 8., 9. e 10. Pagamento
        FormaPagamento formaPagamento = formasPagamento.buscar(pedido.formaPagamento())
                .orElseThrow(() -> new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA));
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        if (!formaPagamento.parcelamentoPermitido(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!formaPagamento.disponivel(totalPedido)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        ResultadoPagamento pagamento = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal ajustePagamento = Moeda.emCentavos(pagamento.totalFinal().subtract(totalPedido));

        return new ResumoCompraResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajustePagamento,
                pagamento.totalFinal(),
                pagamento.parcelas(),
                pagamento.valorParcela(),
                nivelClube.calcularCredito(subtotalProdutos),
                nivelClube.temBrinde(subtotalProdutos));
    }

    private List<ItemCarrinho> lerItens(ResumoCompraRequest pedido) {
        if (pedido.itens() == null || pedido.itens().isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        List<ItemCarrinho> itens = new ArrayList<>(pedido.itens().size());
        for (ItemRequest item : pedido.itens()) {
            itens.add(lerItem(item));
        }
        return List.copyOf(itens);
    }

    private ItemCarrinho lerItem(ItemRequest item) {
        if (item == null
                || !positivo(item.precoUnitario())
                || !positivo(item.pesoKg())
                || item.quantidade() == null
                || item.quantidade() <= 0) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new ItemCarrinho(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal somarProdutos(List<ItemCarrinho> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            subtotal = subtotal.add(item.totalLinha());
        }
        return Moeda.emCentavos(subtotal);
    }

    /** Peso do pedido: soma de peso x quantidade, sem arredondar. */
    private BigDecimal somarPeso(List<ItemCarrinho> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            peso = peso.add(item.pesoLinha());
        }
        return peso;
    }

    private BigDecimal calcularDescontoCupom(String codigo,
                                             List<ItemCarrinho> itens,
                                             BigDecimal subtotalProdutos,
                                             BigDecimal frete) {
        if (codigo == null || codigo.isBlank()) {
            return Moeda.ZERO;
        }
        Cupom cupom = cupons.buscar(codigo)
                .orElseThrow(() -> new CheckoutException(CodigoErro.CUPOM_INVALIDO));
        DadosCupom dados = new DadosCupom(itens, subtotalProdutos, frete);
        if (!cupom.aplicavel(dados)) {
            throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return Moeda.emCentavos(cupom.calcularDesconto(dados));
    }
}
