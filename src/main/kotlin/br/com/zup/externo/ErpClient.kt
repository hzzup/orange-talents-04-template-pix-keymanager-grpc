package br.com.zup.externo

import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client

@Client("\${externo.erp}")
interface ErpClient {

    //consultando cliente por id, é retornado o cliente do ERP itau
    @Get(value = "/api/v1/clientes/{clienteId}", consumes = [MediaType.APPLICATION_JSON])
    fun consultaClienteId(@QueryValue("clienteId") clienteId : String?) : HttpResponse<ClientErpResponse>
}

//Classe que de fato virá como resposta para nós, com os dados do cliente
class ClientErpResponse(val nome:String, val cpf:String, val instituicao:Instituicao)

//classe para pegarmos os dados da instituicao
class Instituicao(val nome:String, val ispb:String)