package br.com.zup.unidade

import br.com.zup.pix.TipoDeChave
import br.com.zup.pix.TipoDeConta
import br.com.zup.pix.model.ChavePix
import br.com.zup.pix.model.ContaAssociada
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

internal class ChavePixTest {

    companion object {
        val RANDOM_BCB_KEY_VALUE = UUID.randomUUID().toString()
    }

    @Test
    fun `deve atualizar chave aleatoria pelo valor da chave bcb`(){
        //cenario
        val chaveAleatoria = ChavePix(
            clienteId = UUID.randomUUID(),
            tipo = TipoDeChave.ALEATORIA,
            chave = UUID.randomUUID().toString(),
            tipoDeConta = TipoDeConta.CONTA_CORRENTE,
            conta = ContaAssociada(
                instituicao = "Teste",
                nomeDoTitular = "Teste",
                cpfDoTitular = "26896912057",
                agencia = "54",
                numeroDaConta = "4444"
            )
        )

        //acao
        chaveAleatoria.atualizaChaveAleatoria(RANDOM_BCB_KEY_VALUE)

        //validacao
        assertEquals(chaveAleatoria.chave, RANDOM_BCB_KEY_VALUE)
    }

    @Test
    fun `nao deve atualizar chave que nao seja aleatoria`(){
        //cenario
        val chaveNaoAleatoria = ChavePix(
            clienteId = UUID.randomUUID(),
            tipo = TipoDeChave.CPF,
            chave = "26896912057",
            tipoDeConta = TipoDeConta.CONTA_CORRENTE,
            conta = ContaAssociada(
                instituicao = "Teste",
                nomeDoTitular = "Teste",
                cpfDoTitular = "26896912057",
                agencia = "54",
                numeroDaConta = "4444"
            )
        )

        //acao
        chaveNaoAleatoria.atualizaChaveAleatoria(RANDOM_BCB_KEY_VALUE)

        //validacao
        assertNotEquals(chaveNaoAleatoria.chave, RANDOM_BCB_KEY_VALUE)
    }
}