package br.com.zup.servicosGrpc.validacoes

import br.com.zup.PixRequest.TipoChave
import br.com.zup.PixRequest.TipoChave.*
import br.com.zup.PixRequest.TipoConta
import io.grpc.Status
import io.micronaut.http.HttpStatus

fun VerificaIdClienteInvalido(idCliente : String?) : Boolean {
    if (idCliente == null || idCliente.isBlank()) return true
    return false
}

fun VerificaClienteErpItau(status : HttpStatus) : Boolean {
    if (status != HttpStatus.OK) return true
    return false
}

fun VerificaTipoEValorChave(tipo : TipoChave?, valor : String?) : Validado {
    var httpError = Status.INVALID_ARGUMENT
    var descricao = ""
    var verificaErro: Boolean? = false
    when (tipo) {
        CPF -> {
            descricao = "Cpf invalido"
            verificaErro = valor?.let {
                it.matches("^[0-9]{11}\$".toRegex())
            }
        }
        CELULAR -> {
            descricao = "Celular invalido"
            verificaErro = valor?.let {
                it.matches("^\\+[1-9][0-9]\\d{1,14}\$".toRegex())
            }
        }
        EMAIL -> {
            descricao = "Email invalido"
            verificaErro = valor?.let {
                //regex pego na internet, para nao ter que implementar diretamente na mao
                it.matches("(?:[a-z0-9!#\$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#\$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])".toRegex())
            }
        }
        ALEATORIA -> {
            descricao = "Valor não deve ser inserido na chave aleatória"
            verificaErro = valor?.let {
                it.isBlank()
            }
        }
        else -> {
            return Validado(false,httpError,"Tipo de Chave invalida")
        }

    }
    return Validado(verificaErro, httpError, descricao)
}

fun VerificaTipoConta(tipo : TipoConta?) : Validado {
    var httpError = Status.INVALID_ARGUMENT
    var descricao = ""
    var verificaErro: Boolean? = false
    if (tipo!! !in TipoConta.CORRENTE..TipoConta.POUPANCA) {
        verificaErro = false
        descricao = "Tipo de conta invalido"
    } else {
        verificaErro = true
    }
    return Validado(verificaErro, httpError, descricao)
}

class Validado(val semErro : Boolean?, val status :Status, val descricao : String)