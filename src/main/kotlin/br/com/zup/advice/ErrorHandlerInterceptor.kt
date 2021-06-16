package br.com.zup.advice

import io.grpc.BindableService
import io.grpc.stub.StreamObserver
import io.micronaut.aop.MethodInterceptor
import io.micronaut.aop.MethodInvocationContext
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
//classes necessarias para extender
class ExceptionHandlerInterceptor(@Inject private val resolver: ExceptionHandlerResolver) :
    MethodInterceptor<BindableService, Any?> {

    private val logger = LoggerFactory.getLogger(this::class.java)

    //metodo que intercepta as chamadas
    override fun intercept(context: MethodInvocationContext<BindableService, Any?>): Any? {
        try {
            //continue caso não seja encontrado nenhum erro
            return context.proceed()
        } catch (e: Exception) {

            logger.error("Handling the exception '${e.javaClass.name}' while processing the call: ${context.targetMethod}", e)

            @Suppress("UNCHECKED_CAST")
            val handler = resolver.resolve(e) as ExceptionHandler<Exception>
            val status = handler.handle(e)

            //retorna uma runtime exception com a mensagem, essa excecao é do tipo que o grpc devolve
            GrpcEndpointArguments(context).response()
                .onError(status.asRuntimeException())

            return null
        }
    }

    //argumentos recebidos pelo metodo do endpoint
    private class GrpcEndpointArguments(val context : MethodInvocationContext<BindableService, Any?>) {

        fun response(): StreamObserver<*> {
            return context.parameterValues[1] as StreamObserver<*>
        }
    }
}