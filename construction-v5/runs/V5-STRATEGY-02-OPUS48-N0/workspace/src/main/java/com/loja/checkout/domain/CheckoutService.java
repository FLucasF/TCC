package com.loja.checkout.domain;

import com.loja.checkout.api.dto.CheckoutRequest;
import com.loja.checkout.api.dto.ItemRequest;
import com.loja.checkout.api.dto.ResumoResponse;
import com.loja.checkout.domain.cupom.ContextoCupom;
import com.loja.checkout.domain.cupom.Cupom;
import com.loja.checkout.domain.cupom.RegistroCupons;
import com.loja.checkout.domain.entrega.ModalidadeEntrega;
import com.loja.checkout.domain.entrega.RegistroModalidadesEntrega;
import com.loja.checkout.domain.pagamento.FormaPagamento;
import com.loja.checkout.domain.pagamento.RegistroFormasPagamento;
import com.loja.checkout.domain.pagamento.ResultadoPagamento;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Calcula o resumo da compra. Segue a ordem de validacao e a ordem de calculo
 * combinadas com o financeiro, e devolve o primeiro problema encontrado.
 */
@Service
public class CheckoutService {

    private final RegistroModalidadesEntrega modalidades;
    private final RegistroCupons cupons;
    private final RegistroFormasPagamento formasPagamento;

    public CheckoutService(RegistroModalidadesEntrega modalidades,
                           RegistroCupons cupons,
                           RegistroFormasPagamento formasPagamento) {
        this.modalidades = modalidades;
        this.cupons = cupons;
        this.formasPagamento = formasPagamento;
    }

    public ResumoResponse calcular(CheckoutRequest req) {
        // 1. Itens do carrinho.
        List<Item> itens = validarItens(req.itens());

        // 2. Nivel do clube. 3. Regiao. 4. Modalidade de entrega.
        NivelClube nivel = NivelClube.de(req.nivelClube());
        Regiao regiao = Regiao.de(req.regiao());
        ModalidadeEntrega modalidade = modalidades.buscar(req.modalidadeEntrega());

        // 5. A entrega atende este peso? (ex.: motoboy acima de 5 kg).
        BigDecimal pesoTotal = pesoTotal(itens);
        if (!modalidade.atende(pesoTotal)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        // Valor dos produtos, frete e seguro.
        BigDecimal subtotal = subtotalProdutos(itens);
        BigDecimal frete = nivel.temFreteGratis()
                ? Dinheiro.ZERO
                : Dinheiro.centavos(modalidade.calcularFrete(pesoTotal));
        BigDecimal seguro = regiao.calcularSeguro(subtotal);

        // 6. Cupom (se veio). 7. Cupom aplicavel ao pedido.
        BigDecimal descontoCupom = Dinheiro.ZERO;
        if (temCupom(req.cupom())) {
            Cupom cupom = cupons.buscar(req.cupom());
            ContextoCupom contexto = new ContextoCupom(subtotal, itens, frete);
            if (!cupom.aplicavel(contexto)) {
                throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
            }
            descontoCupom = Dinheiro.centavos(cupom.calcularDesconto(contexto));
        }

        // Total do pedido = produtos - desconto + frete + seguro.
        BigDecimal totalPedido = Dinheiro.centavos(
                subtotal.subtract(descontoCupom).add(frete).add(seguro));

        // 8. Forma de pagamento. 9. Parcelamento. 10. Forma atende o pedido.
        FormaPagamento pagamento = formasPagamento.buscar(req.formaPagamento());
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!pagamento.parcelamentoValido(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!pagamento.atende(totalPedido)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);

        // Clube: credito para a proxima compra e brinde.
        BigDecimal credito = nivel.calcularCredito(subtotal);
        boolean brinde = nivel.ganhaBrinde(subtotal);

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                resultado.ajuste(),
                resultado.totalFinal(),
                resultado.parcelas(),
                resultado.valorParcela(),
                credito,
                brinde);
    }

    private List<Item> validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        List<Item> validados = new ArrayList<>(itens.size());
        for (ItemRequest item : itens) {
            if (item == null
                    || !positivo(item.precoUnitario())
                    || !positivo(item.quantidade())
                    || !positivo(item.pesoKg())) {
                throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
            }
            validados.add(new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return validados;
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean positivo(Integer valor) {
        return valor != null && valor > 0;
    }

    private BigDecimal subtotalProdutos(List<Item> itens) {
        BigDecimal soma = BigDecimal.ZERO;
        for (Item item : itens) {
            soma = soma.add(item.valor());
        }
        return Dinheiro.centavos(soma);
    }

    private BigDecimal pesoTotal(List<Item> itens) {
        BigDecimal soma = BigDecimal.ZERO;
        for (Item item : itens) {
            soma = soma.add(item.peso());
        }
        return soma;
    }

    private boolean temCupom(String cupom) {
        return cupom != null && !cupom.isBlank();
    }
}
