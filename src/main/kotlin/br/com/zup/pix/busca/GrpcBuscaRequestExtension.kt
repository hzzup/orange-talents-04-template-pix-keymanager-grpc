package br.com.zup.pix.busca

import br.com.zup.BuscaChavePixRequest
import br.com.zup.BuscaChavePixRequest.FiltroCase
import br.com.zup.BuscaChavePixRequest.FiltroCase.*
import br.com.zup.RegistraChavePixRequest
import br.com.zup.pix.TipoDeChave
import br.com.zup.TipoDeChave.*
import br.com.zup.pix.TipoDeConta
import br.com.zup.TipoDeConta.*
import javax.validation.ConstraintViolationException
import javax.validation.Validator

//extension function da classe gerada pelo proto
fun BuscaChavePixRequest.toModel(validator : Validator) : Filtro {
    //aqui validamos os filtros que definimos lá no oneof do proto
    val filtro = when(filtroCase) {
        //se for por PixID então usamos o filtro de por pix id
        PIXID -> pixId.let {
            Filtro.PorPixId(clienteId = it.clienteId, pixId = it.pixId)
        }
        //se não, usamos o filtro por chave
        CHAVE -> Filtro.PorChave(chave)
        //se for passado valores invalidos ou vazios nós passamos no filtro invalido
        FILTRO_NOT_SET -> Filtro.Invalido()
    }

    //se for encontrado alguma excecao pelo validator jogamos uma exception
    val violations = validator.validate(filtro)
    if (violations.isNotEmpty()) {
        throw ConstraintViolationException(violations);
    }

    return filtro
}