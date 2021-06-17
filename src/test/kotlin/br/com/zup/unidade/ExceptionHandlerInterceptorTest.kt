package br.com.zup.unidade

import br.com.zup.advice.ExceptionHandlerInterceptor
import br.com.zup.advice.ExceptionHandlerResolver
import io.grpc.BindableService
import io.grpc.stub.StreamObserver
import io.micronaut.aop.MethodInvocationContext
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import java.lang.RuntimeException
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
internal class ExceptionHandlerInterceptorTest {

    //mockamos o tipo de chamada que o metodo espera
    //ExceptionHandlerInterceptor << assinatura do metodo
    @Mock
    lateinit var context: MethodInvocationContext<BindableService, Any?>

    val interceptor = ExceptionHandlerInterceptor(resolver = ExceptionHandlerResolver(handlers = emptyList()))

    @Test //mockamos o stream observer também nesse metodo
    fun `deve capturar a excecao lancada e gerar erro no gRPC`(@Mock streamObserver : StreamObserver<*>) {
        with(context) {
            `when`(proceed()).thenThrow(RuntimeException("Erro qualquer"))
            `when`(parameterValues).thenReturn(arrayOf(null,streamObserver))
        }
        interceptor.intercept(context)
        verify(streamObserver).onError(notNull())
    }

    @Test
    fun `nao deve gerar execao se o metodo esta ok deve apenas continuar`() {
        val expected = "valor qualquer"

        `when`(context.proceed()).thenReturn(expected)

        Assertions.assertEquals(expected,interceptor.intercept(context))
    }
}