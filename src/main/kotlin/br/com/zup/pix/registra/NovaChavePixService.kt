package br.com.zup.pix.registra

import br.com.zup.pix.ChavePixRepository
import br.com.zup.pix.exceptions.ChavePixExistenteException
import br.com.zup.pix.externo.ContasDeClientesNoItauClient
import br.com.zup.pix.model.ChavePix
import io.micronaut.validation.Validated
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.Valid

@Validated // essa classe será validada de acordo com as anotacoes do dto
@Singleton
class NovaChavePixService(@Inject val repository: ChavePixRepository,
                          @Inject val itauClient: ContasDeClientesNoItauClient
) {

    private val LOGGER = LoggerFactory.getLogger(this::class.java)

    @Transactional //transactional pois salvará nossa chave pix no banco
    //retorna nossa entidade modelo de chave pix
    fun registra(@Valid novaChave: NovaChavePix): ChavePix {

        // Verifica se chave já existe no sistema, se ja existe, throws exception
        if (repository.existsByChave(novaChave.chave))
            throw ChavePixExistenteException("Chave Pix '${novaChave.chave}' existente")

        // Busca dados da conta no ERP do ITAU - se não retornar um body, requisicao falhou, throws exception
        val response = itauClient.buscaContaPorTipo(novaChave.clienteId!!, novaChave.tipoDeConta!!.name)
        val conta = response.body()?.toModel() ?: throw IllegalStateException("Cliente não encontrado no Itau")

        // Grava no banco de dados
        val chave = novaChave.toModel(conta)
        repository.save(chave)

        return chave
    }
}