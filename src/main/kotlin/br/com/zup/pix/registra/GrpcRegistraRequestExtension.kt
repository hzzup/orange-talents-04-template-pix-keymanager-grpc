package br.com.zup.pix.registra

import br.com.zup.RegistraChavePixRequest
import br.com.zup.pix.TipoDeChave
import br.com.zup.TipoDeChave.*
import br.com.zup.pix.TipoDeConta
import br.com.zup.TipoDeConta.*


//extension function da classe gerada pelo proto
fun RegistraChavePixRequest.toModel() : NovaChavePix {
    return NovaChavePix( // 1
        clienteId = clienteId,
        tipo = when (tipoDeChave) {
            //registrado que esse enum é gerado pelo proto
            UNKNOWN_TIPO_CHAVE -> null
            else -> TipoDeChave.valueOf(tipoDeChave.name) // 1
        },
        chave = chave,
        tipoDeConta = when (tipoDeConta) {
            //registrado que esse enum é gerado pelo proto
            UNKNOWN_TIPO_CONTA -> null
            else -> TipoDeConta.valueOf(tipoDeConta.name) // 1
        }
    )
}