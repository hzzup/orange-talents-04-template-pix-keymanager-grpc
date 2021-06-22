package br.com.zup.pix.busca

import br.com.zup.pix.ChavePixRepository
import br.com.zup.pix.exceptions.ChavePixNaoEncontradaException
import br.com.zup.pix.externo.BcbClient
import br.com.zup.pix.validation.ValidUUID
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.HttpStatus
import java.util.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@Introspected
sealed class Filtro {

    //metodo abstrato da classe Filtro, ele deve ser implementado por todos que estenderem dessa classe
    abstract fun filtra(repository: ChavePixRepository, bcbClient: BcbClient): ChavePixInfo

    //classe que estende de Filtro, possui seu próprio filtro por Pix Id
    //esse tipo de busca é dentro do nosso sistema keymanager
    @Introspected
    data class PorPixId(
        @field:NotBlank @field:ValidUUID val clienteId: String,
        @field:NotBlank @field:ValidUUID val pixId: String,
    ) : Filtro() {

        fun pixIdAsUuid() = UUID.fromString(pixId)
        fun clienteIdAsUuid() = UUID.fromString(clienteId)

        override fun filtra(repository: ChavePixRepository, bcbClient: BcbClient): ChavePixInfo {
            return repository.findById(pixIdAsUuid()) //acha a chave pelo pixId (id gerado pelo meu banco)
                .filter { it.chavePertenceAo(clienteIdAsUuid()) } //verifica se essa chave é a do cliente passado
                .map(ChavePixInfo::of) // se encontrado transforma a classe modelo em um dto de resposta
                .orElseThrow { ChavePixNaoEncontradaException("Chave Pix não encontrada") } //caso contrario chave não foi encontrada
        }
    }

    //classe que estende de Filtro, possui seu próprio filtro pela propria chave
    //esse tipo de busca é para sistemas externos, tanto que caso não encontremos as chaves
    //procuramos ela no BCB
    @Introspected
    data class PorChave(@field:NotBlank @Size(max = 77) val chave: String) : Filtro() { // 1

        override fun filtra(repository: ChavePixRepository, bcbClient: BcbClient): ChavePixInfo {
            return repository.findByChave(chave) //procura essa chave no nosso banco
                .map(ChavePixInfo::of) //se a chave for encontrada transforma numa response (dto)
                .orElseGet { //caso contrario procuramos no BCB
                    val response = bcbClient.buscaChave(chave) //busca a chave no BCB
                    when (response.status) { //verifica pelo status da resposta 200
                        HttpStatus.OK -> response.body()?.toModel() //se encontrou 200 ok, transforma no modelo ChavePixInfo
                        else -> throw ChavePixNaoEncontradaException("Chave Pix não encontrada")//status != 200 erro
                    }
                }
        }
    }

    //classe que estende de Filtro para casos invalidos
    @Introspected
    class Invalido() : Filtro() {

        override fun filtra(repository: ChavePixRepository, bcbClient: BcbClient): ChavePixInfo {
            throw IllegalArgumentException("Chave Pix inválida ou não informada")
        }
    }
}
