package br.com.zup.pix.busca

import br.com.zup.pix.TipoDeChave
import br.com.zup.pix.TipoDeConta
import br.com.zup.pix.model.ChavePix
import br.com.zup.pix.model.ContaAssociada
import java.time.LocalDateTime
import java.util.*

data class ChavePixInfo(
    val pixId: UUID? = null,
    val clienteId: UUID? = null,
    val tipo: TipoDeChave,
    val chave: String,
    val tipoDeConta: TipoDeConta,
    val conta: ContaAssociada,
    val registradaEm: LocalDateTime = LocalDateTime.now()
) {

    companion object {
        //esse metodo já converte meu modelo para as informacoes da chave pix
        //é um DTO para eu não retornar minha classe modelo
        fun of(chave: ChavePix): ChavePixInfo {
            return ChavePixInfo(
                pixId = chave.id,
                clienteId = chave.clienteId,
                tipo = chave.tipo,
                chave = chave.chave,
                tipoDeConta = chave.tipoDeConta,
                conta = chave.conta,
                registradaEm = chave.criadaEm
            )
        }
    }
}