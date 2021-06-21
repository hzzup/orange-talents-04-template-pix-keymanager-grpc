package br.com.zup.unidade

import br.com.zup.pix.TipoDeChave
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class TipoDeChaveTest {

    @Nested
    inner class ALEATORIA {
        @Test
        fun `deve ser valido quando chave aleatoria for nula ou vazia ou com espacos em branco`() {
            with(TipoDeChave.ALEATORIA) {
                assertTrue(valida(null))
                assertTrue(valida(""))
                assertTrue(valida("                              "))
            }
        }
        @Test
        fun `nao deve ser valido quando chave aleatoria ter valor`() {
            with(TipoDeChave.ALEATORIA) {
                assertFalse(valida("teste com valor"))
            }
        }
    }

    @Nested
    inner class EMAIL {
        @Test
        fun `nao deve ser valido quando chave de email for nula ou vazia ou com espacos em branco`() {
            with(TipoDeChave.EMAIL) {
                assertFalse(valida(null))
                assertFalse(valida(""))
                assertFalse(valida("                                 "))
            }
        }

        @Test
        fun `nao deve ser valido quando for email mal formatado`() {
            with(TipoDeChave.EMAIL) {
                assertFalse(valida("valor teste"))
            }
        }

        @Test
        fun `deve ser valido quando email for email bem formatado`() {
            with(TipoDeChave.EMAIL) {
                assertTrue(valida("valido@teste.com.br"))
            }
        }
    }

    @Nested
    inner class CELULAR {

        @Test
        fun `nao deve ser valido quando chave de celular for nula ou vazia ou com espacos em branco`() {
            with(TipoDeChave.CELULAR) {
                assertFalse(valida(null))
                assertFalse(valida(""))
                assertFalse(valida("                            "))
            }
        }

        @Test
        fun `nao deve ser valido com formato de celular errado`() {
            with(TipoDeChave.CELULAR){
                assertFalse(valida("teste nao valido"))
            }
        }

        @Test
        fun `deve ser valido com formato de celular correto`() {
            with(TipoDeChave.CELULAR) {
                assertTrue(valida("+1122333334444"))
            }
        }

    }

    @Nested
    inner class CPF{

        @Test
        fun `nao deve ser valido com chave cpf nula ou vazia ou com espacos em branco`() {
            with(TipoDeChave.CPF) {
                assertFalse(valida(null))
                assertFalse(valida(""))
                assertFalse(valida("                 "))
            }
        }

        @Test
        fun `nao deve ser valido com cpf invalido`() {
            with(TipoDeChave.CPF) {
                assertFalse(valida("valor invalido"))
            }
        }

        @Test
        fun `deve ser valido com cpf em formato correto`() {
            with(TipoDeChave.CPF) {
                assertTrue(valida("26896912057"))
            }
        }


    }

}