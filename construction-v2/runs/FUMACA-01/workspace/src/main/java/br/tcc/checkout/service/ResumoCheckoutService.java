package br.tcc.checkout.service;

import br.tcc.checkout.api.dto.ItemDto;
import br.tcc.checkout.api.dto.ResumoCheckoutRequest;
import br.tcc.checkout.api.dto.ResumoCheckoutResponse;
import br.tcc.checkout.domain.Arredondador;
import br.tcc.checkout.domain.cupom.Cupom;
import br.tcc.checkout.domain.entrega.Modalidade;
import br.tcc.checkout.domain.pagamento.FormaPagamento;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ResumoCheckoutService {

    public ResumoCheckoutResponse calcularResumo(ResumoCheckoutRequest request) {
        List<ItemDto> itens = request.itens();
        String modalidadeEntrega = request.modalidadeEntrega();
        String cupomCodigo = request.cupom();
        String formaPagamentoNome = request.formaPagamento();
        int parcelas = request.parcelas() != null ? request.parcelas() : 1;

        validarPedido(itens);
        Modalidade modalidade = validarModalidade(modalidadeEntrega);
        Cupom cupom = validarCupom(cupomCodigo);
        FormaPagamento formaPagamento = validarFormaPagamento(formaPagamentoNome);

        double pesoTotal = calcularPeso(itens);
        validarDisponibilidadeModalidade(modalidade, pesoTotal);

        double subtotal = calcularSubtotal(itens);
        validarAplicabilidadeCupom(cupom, subtotal);

        double desconto = 0.0;
        if (cupom == Cupom.LEVE3PAGUE2) {
            desconto = calcularDescontoLeve3Pague2(itens);
        } else if (cupom != null) {
            desconto = cupom.aplicar(subtotal, 0);
        }
        desconto = Arredondador.arredondar(desconto);

        double frete = Arredondador.arredondar(modalidade.calcularFrete(pesoTotal));
        if (cupom == Cupom.FRETEGRATIS) {
            desconto = frete;
            frete = 0.0;
        }

        double totalPedido = Arredondador.arredondar(subtotal - desconto + frete);
        validarParcelamento(formaPagamento, parcelas);
        validarDisponibilidadeFormaPagamento(formaPagamento, totalPedido);

        double ajuste = formaPagamento.calcularAjuste(totalPedido, parcelas);
        ajuste = Arredondador.arredondar(ajuste);

        double totalFinal = Arredondador.arredondar(totalPedido + ajuste);
        double valorParcela = Arredondador.arredondar(totalFinal / parcelas);

        return new ResumoCheckoutResponse(
            subtotal,
            desconto,
            frete,
            modalidade.getPrazo(),
            ajuste,
            totalFinal,
            parcelas,
            valorParcela
        );
    }

    private void validarPedido(List<ItemDto> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new ErroValidacao("PEDIDO_INVALIDO");
        }
        for (ItemDto item : itens) {
            if (item.precoUnitario() <= 0 || item.quantidade() <= 0 || item.pesoKg() <= 0) {
                throw new ErroValidacao("PEDIDO_INVALIDO");
            }
        }
    }

    private Modalidade validarModalidade(String modalidadeNome) {
        if (modalidadeNome == null || modalidadeNome.isBlank()) {
            throw new ErroValidacao("MODALIDADE_INVALIDA");
        }
        try {
            return Modalidade.valueOf(modalidadeNome.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ErroValidacao("MODALIDADE_INVALIDA");
        }
    }

    private void validarDisponibilidadeModalidade(Modalidade modalidade, double pesoTotal) {
        if (!modalidade.ehDisponivelPara(pesoTotal)) {
            throw new ErroValidacao("MODALIDADE_INDISPONIVEL");
        }
    }

    private Cupom validarCupom(String cupomCodigo) {
        if (cupomCodigo == null) {
            return null;
        }
        Cupom cupom = Cupom.porCodigo(cupomCodigo);
        if (cupom == null) {
            throw new ErroValidacao("CUPOM_INVALIDO");
        }
        return cupom;
    }

    private void validarAplicabilidadeCupom(Cupom cupom, double subtotal) {
        if (cupom != null && !cupom.eAplicavel(subtotal, 0)) {
            throw new ErroValidacao("CUPOM_NAO_APLICAVEL");
        }
    }

    private FormaPagamento validarFormaPagamento(String formaPagamentoNome) {
        if (formaPagamentoNome == null || formaPagamentoNome.isBlank()) {
            throw new ErroValidacao("FORMA_PAGAMENTO_INVALIDA");
        }
        FormaPagamento formaPagamento = FormaPagamento.porNome(formaPagamentoNome);
        if (formaPagamento == null) {
            throw new ErroValidacao("FORMA_PAGAMENTO_INVALIDA");
        }
        return formaPagamento;
    }

    private void validarParcelamento(FormaPagamento formaPagamento, int parcelas) {
        if (!formaPagamento.eParcelamentoValido(parcelas)) {
            throw new ErroValidacao("PARCELAMENTO_INVALIDO");
        }
    }

    private void validarDisponibilidadeFormaPagamento(FormaPagamento formaPagamento, double total) {
        if (!formaPagamento.eDisponivelPara(total)) {
            throw new ErroValidacao("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private double calcularSubtotal(List<ItemDto> itens) {
        double subtotal = 0.0;
        for (ItemDto item : itens) {
            subtotal += item.precoUnitario() * item.quantidade();
        }
        return Arredondador.arredondar(subtotal);
    }

    private double calcularDescontoLeve3Pague2(List<ItemDto> itens) {
        double desconto = 0.0;
        for (ItemDto item : itens) {
            int unidadesGratis = item.quantidade() / 3;
            desconto += unidadesGratis * item.precoUnitario();
        }
        return desconto;
    }

    private double calcularPeso(List<ItemDto> itens) {
        double peso = 0.0;
        for (ItemDto item : itens) {
            peso += item.pesoKg() * item.quantidade();
        }
        return peso;
    }

    public static class ErroValidacao extends RuntimeException {
        private final String codigo;

        public ErroValidacao(String codigo) {
            super(codigo);
            this.codigo = codigo;
        }

        public String getCodigo() {
            return codigo;
        }
    }
}
