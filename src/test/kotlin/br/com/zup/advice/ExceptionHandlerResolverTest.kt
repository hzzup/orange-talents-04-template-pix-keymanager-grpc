package br.com.zup.advice

import br.com.zup.advice.ExceptionHandler
import br.com.zup.advice.ExceptionHandlerResolver
import br.com.zup.advice.handlers.DefaultExceptionHandler
import junit.framework.Assert.assertSame
import junit.framework.Assert.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class ExceptionHandlerResolverTest {

    //posteriormente inicializaremos nosso exceptionhandler com a IllegalArgumentException
    lateinit var illegalArgumentExceptionHandler: ExceptionHandler<IllegalArgumentException>

    //tambem nosso resolver que tratará as exceptions
    lateinit var resolver: ExceptionHandlerResolver

    @BeforeEach
    fun setup() {
        illegalArgumentExceptionHandler = object : ExceptionHandler<IllegalArgumentException> {

            override fun handle(e: IllegalArgumentException): ExceptionHandler.StatusWithDetails {
                TODO("Not yet implemented")
            }

            override fun supports(e: Exception) = e is java.lang.IllegalArgumentException
        }

        resolver = ExceptionHandlerResolver(handlers = listOf(illegalArgumentExceptionHandler))
    }

    @Test
    fun `deve retornar o ExceptionHandler especifico para o tipo de excecao`() {
        val resolved = resolver.resolve(IllegalArgumentException())

        assertSame(illegalArgumentExceptionHandler, resolved)
    }

    @Test
    fun `deve retornar o ExceptionHandler padrao quando nenhum handler suportar o tipo da excecao`() {
        val resolved = resolver.resolve(RuntimeException())

        assertTrue(resolved is DefaultExceptionHandler)
    }

    @Test
    fun `deve lancar um erro caso encontre mais de um ExceptionHandler que suporte a mesma excecao`() {
        resolver = ExceptionHandlerResolver(listOf(illegalArgumentExceptionHandler, illegalArgumentExceptionHandler))

        assertThrows<IllegalStateException> { resolver.resolve(IllegalArgumentException()) }
    }
}