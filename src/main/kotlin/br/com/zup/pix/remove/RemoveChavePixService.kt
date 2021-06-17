package br.com.zup.pix.remove

import br.com.zup.pix.ChavePixRepository
import br.com.zup.pix.exceptions.ChavePixNaoEncontradaException
import br.com.zup.pix.validation.ValidUUID
import io.micronaut.validation.Validated
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.constraints.NotBlank

@Validated
@Singleton
class RemoveChavePixService(@Inject val repository : ChavePixRepository) {

    @Transactional
    fun remove(
        @NotBlank @ValidUUID(message = "cliente ID com formato inválido") pixId : String,
        @NotBlank @ValidUUID(message = "pix ID com formato inválido") clienteId : String) {

        val uuidPixId = UUID.fromString(pixId)
        val uuidClienteId = UUID.fromString(clienteId)

        val chave = repository.findByIdAndClienteId(uuidPixId, uuidClienteId)
            .orElseThrow{ ChavePixNaoEncontradaException("Chave pix nao encontrado ou nao associada a um cliente") }

        repository.deleteById(uuidPixId)
    }
}