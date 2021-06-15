package br.com.zup.servicosGrpc

import br.com.zup.PixRequest
import br.com.zup.PixRequest.*
import br.com.zup.PixRequest.TipoChave.*
import br.com.zup.PixRequest.TipoConta.*
import javax.persistence.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Entity
data class Pix(
    @field:NotBlank @Column(nullable=false) val agencia : String,
    @field:NotBlank @Column(nullable=false) val numero: String,
    @field:NotBlank @Column(nullable=false) val idCliente : String,
    @field:NotBlank @Column(nullable=false) val nome: String,
    @field:NotBlank @Column(nullable=false) val cpf: String,
    @field:NotBlank @Column(nullable=false) val instituicao: String,
    @field:NotBlank @Column(nullable=false) val ispb: String,
    @field:NotNull @field:Enumerated(EnumType.STRING) @Column(nullable=false) val conta : Conta,
    @field:NotNull @field:Enumerated(EnumType.STRING)  @Column(nullable=false) val tipoPix : TipoPix,
    @field:NotBlank @Column(nullable=false, unique = true) val valorChave : String
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
}

enum class Conta{
    CONTA_CORRENTE, CONTA_POUPANCA
}

enum class TipoPix {
    CPF, CELULAR, EMAIL, ALEATORIA
}


fun contaGrpcParaModelo(tipoConta: TipoConta?): Conta {
    var conta: Conta = Conta.CONTA_CORRENTE
    when(tipoConta) {
        POUPANCA -> {conta = Conta.CONTA_POUPANCA}
    }
    return conta;
}

fun chavePixGrpcParaModelo(tipoChave: PixRequest.TipoChave?): TipoPix {
    var tipoPix : TipoPix = TipoPix.CPF
    when(tipoChave) {
        CELULAR -> {tipoPix = TipoPix.CELULAR}
        EMAIL -> {tipoPix = TipoPix.EMAIL}
        ALEATORIA -> {tipoPix = TipoPix.ALEATORIA}
    }
    return tipoPix
}