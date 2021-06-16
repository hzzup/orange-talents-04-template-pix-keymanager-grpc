package br.com.zup.pix.registra

import br.com.zup.pix.TipoDeChave
import br.com.zup.pix.TipoDeConta
import br.com.zup.pix.model.ChavePix
import br.com.zup.pix.model.ContaAssociada
import br.com.zup.pix.validation.ValidPixKey
import br.com.zup.pix.validation.ValidUUID
import io.micronaut.core.annotation.Introspected
import java.util.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@ValidPixKey // validacao para verificar o valor da chave com o tipo de chave
@Introspected
data class NovaChavePix(
    @field:ValidUUID //validar se é um UUID valido com regex
    @field:NotBlank
    val clienteId: String?,
    @field:NotNull
    val tipo: TipoDeChave?,
    @field:Size(max = 77)
    val chave: String?,
    @field:NotNull
    val tipoDeConta: TipoDeConta?
) {

    fun toModel(conta: ContaAssociada): ChavePix {
        return ChavePix(
            clienteId = UUID.fromString(this.clienteId),
            tipo = TipoDeChave.valueOf(this.tipo!!.name),
            chave = if (this.tipo == TipoDeChave.ALEATORIA) UUID.randomUUID().toString() else this.chave!!,
            tipoDeConta = TipoDeConta.valueOf(this.tipoDeConta!!.name),
            conta = conta
        )
    }

}