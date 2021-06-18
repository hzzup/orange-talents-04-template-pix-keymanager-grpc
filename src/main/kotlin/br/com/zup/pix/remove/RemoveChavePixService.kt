package br.com.zup.pix.remove

import br.com.zup.pix.ChavePixRepository
import br.com.zup.pix.exceptions.ChavePixNaoEncontradaException
import br.com.zup.pix.externo.BcbClient
import br.com.zup.pix.externo.DeletePixKeyRequest
import br.com.zup.pix.model.ContaAssociada
import br.com.zup.pix.validation.ValidUUID
import io.micronaut.http.HttpStatus
import io.micronaut.validation.Validated
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.constraints.NotBlank

@Validated
@Singleton
class RemoveChavePixService(@Inject val repository : ChavePixRepository,
                            @Inject val bcbClient: BcbClient) {

    @Transactional
    fun remove(
        @NotBlank @ValidUUID(message = "cliente ID com formato inválido") pixId : String,
        @NotBlank @ValidUUID(message = "pix ID com formato inválido") clienteId : String) {

        val uuidPixId = UUID.fromString(pixId)
        val uuidClienteId = UUID.fromString(clienteId)

        val chave = repository.findByIdAndClienteId(uuidPixId, uuidClienteId)
            .orElseThrow{ ChavePixNaoEncontradaException("Chave pix nao encontrado ou nao associada a um cliente") }

        //se encontrarmos a chave primeiro tentamos excluir no BCB e só depois na nossa aplicacao
        val respostaBcb = bcbClient.deletaChaveBcb(
            keyPath = chave.chave,
            request = DeletePixKeyRequest(chave.chave, ContaAssociada.ISPB)
        )

        if (respostaBcb.status != HttpStatus.OK) {
            throw IllegalStateException("Erro ao remover chave Pix no Banco Central do Brasil (BCB)")
        }

        repository.deleteById(uuidPixId)
    }
}