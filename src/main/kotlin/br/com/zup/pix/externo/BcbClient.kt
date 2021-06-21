package br.com.zup.pix.externo

import br.com.zup.pix.Instituicoes
import br.com.zup.pix.TipoDeChave
import br.com.zup.pix.TipoDeConta
import br.com.zup.pix.busca.ChavePixInfo
import br.com.zup.pix.model.ChavePix
import br.com.zup.pix.model.ContaAssociada
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.client.annotation.Client
import java.lang.IllegalArgumentException
import java.time.LocalDateTime

@Client("\${externo.bcb}")
interface BcbClient {

    @Post(value = "/api/v1/pix/keys")
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_XML)
    fun cadastraChaveBcb(@Body request : CreatePixKeyRequest) : HttpResponse<CreatePixKeyResponse>

    @Delete(value = "/api/v1/pix/keys/{key}")
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_XML)
    fun deletaChaveBcb(@PathVariable(value = "key") keyPath : String, @Body request : DeletePixKeyRequest) : HttpResponse<DeletePixKeyResponse>

    @Get(value = "/api/v1/pix/keys/{key}")
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_XML)
    fun buscaChave(@PathVariable(value= "key") keyPath : String) : HttpResponse<PixKeyDetailsResponse>
}

data class CreatePixKeyRequest(
    val keyType: TipoChaveBcb,
    val key: String,
    val bankAccount: contaBcb,
    val owner: ClienteBcb) {

    companion object {
        //funcao transforma minha chave modelo que ja foi salva no banco
        //em um molde que é pedido pelo servico BCB
        fun of(chave: ChavePix): CreatePixKeyRequest {
            return CreatePixKeyRequest(
                keyType = TipoChaveBcb.by(chave.tipo),
                key = chave.chave,
                bankAccount = contaBcb(
                    participant = ContaAssociada.ISPB,
                    branch = chave.conta.agencia,
                    accountNumber = chave.conta.numeroDaConta,
                    accountType = TipoContaBcb.by(chave.tipoDeConta)
                ),
                owner = ClienteBcb(
                    type = TipoCliente.NATURAL_PERSON,
                    name = chave.conta.nomeDoTitular,
                    taxIdNumber = chave.conta.cpfDoTitular
                )
            )
        }
    }
}

data class ClienteBcb(
    val type: TipoCliente,
    val name: String,
    val taxIdNumber: String
)

//enum do tipo cliente (externo apenas)
enum class TipoCliente{
    NATURAL_PERSON, LEGAL_PERSON;
}

data class contaBcb(
    val participant: String,
    val branch: String,
    val accountNumber: String,
    val accountType : TipoContaBcb
)

//enum para tambem validar o tipo de conta de acordo com a minha classe modelo
enum class TipoContaBcb(val tipoContaModel : TipoDeConta?) {
    CACC(TipoDeConta.CONTA_CORRENTE), //conta corrente
    SVGS(TipoDeConta.CONTA_POUPANCA); //poupanca

    companion object {
        //mapeamos os valores que temos em nosso tipo BCB para os tipos da modelo
        private val accountMap = TipoContaBcb.values().associateBy(TipoContaBcb::tipoContaModel)

        fun by(tipoContaModel: TipoDeConta) : TipoContaBcb{
            return TipoContaBcb.accountMap[tipoContaModel] ?: throw IllegalArgumentException("AccountType invalid or not found for $tipoContaModel")
        }
    }
}

//nesse enum ja validamos com o enum da nossa classe modelo
//colocamos no construtor do enum o tipo referente aos da nossa classe modelo
//fazemos isso para que caso haja alguma alteração ou nova chave, nosso programa não quebre
enum class TipoChaveBcb(val tipoChaveModel : TipoDeChave?) {
    CPF(TipoDeChave.CPF),
    CNPJ(null),
    PHONE(TipoDeChave.CELULAR),
    EMAIL(TipoDeChave.EMAIL),
    RANDOM(TipoDeChave.ALEATORIA);

    companion object {
        //mapeamos nossos valores de chave BCB para o tipo de chave da nossa entidade modelo
        private val keyMap = TipoChaveBcb.values().associateBy(TipoChaveBcb::tipoChaveModel)

        //verificamos se caso seja nulo ou diferente dos tipos de chaves aceito
        //nós jogamos uma excecao de illegal argument
        fun by(tipoChaveModel: TipoDeChave) : TipoChaveBcb {
            return keyMap[tipoChaveModel] ?: throw IllegalArgumentException("PixKeyType invalid or not found for $tipoChaveModel")
        }
    }
}

data class CreatePixKeyResponse (
    val keyType: TipoChaveBcb,
    val key: String,
    val bankAccount: contaBcb,
    val owner: ClienteBcb,
    val createdAt: LocalDateTime
)

data class DeletePixKeyRequest(
    val key : String,
    val participant : String
)

data class DeletePixKeyResponse (
    val key : String,
    val participant: String,
    val deletedAt : LocalDateTime
)

data class PixKeyDetailsResponse (
    val keyType: TipoChaveBcb,
    val key: String,
    val bankAccount: contaBcb,
    val owner: ClienteBcb,
    val createdAt: LocalDateTime
) {
    //transforma a resposta do bcb para o modelo de response do meu sistema (dto)
    fun toModel(): ChavePixInfo {
        return ChavePixInfo(
            tipo = keyType.tipoChaveModel!!,
            chave = this.key,
            tipoDeConta = when(this.bankAccount.accountType) {
                TipoContaBcb.CACC -> TipoDeConta.CONTA_CORRENTE
                TipoContaBcb.SVGS -> TipoDeConta.CONTA_POUPANCA
            },
            conta = ContaAssociada(
                instituicao = Instituicoes.nome(bankAccount.participant),
                nomeDoTitular = owner.name,
                cpfDoTitular = owner.taxIdNumber,
                agencia = bankAccount.branch,
                numeroDaConta = bankAccount.accountNumber
            )
        )
    }
}