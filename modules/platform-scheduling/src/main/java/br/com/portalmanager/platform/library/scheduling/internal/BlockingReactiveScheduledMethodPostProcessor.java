package br.com.portalmanager.platform.library.scheduling.internal;

import br.com.portalmanager.platform.library.scheduling.annotation.DisableScheduledLock;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.scheduling.annotation.Scheduled;

/** Wraps reactive scheduled publishers so the scheduler runnable remains active until termination. */
public final class BlockingReactiveScheduledMethodPostProcessor implements BeanPostProcessor, PriorityOrdered {
    private final ReactiveAdapterRegistry adapters = ReactiveAdapterRegistry.getSharedInstance();

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        List<Method> reactiveScheduledMethods = Arrays.stream(ReflectionUtils.getAllDeclaredMethods(targetClass))
                .filter(method -> method.getAnnotationsByType(Scheduled.class).length > 0
                        || AnnotatedElementUtils.hasAnnotation(method, Scheduled.class))
                .filter(method -> !AnnotatedElementUtils.hasAnnotation(method, DisableScheduledLock.class))
                .filter(method -> adapters.getAdapter(method.getReturnType()) != null)
                .toList();
        if (reactiveScheduledMethods.isEmpty()) {
            return bean;
        }

        ProxyFactory proxyFactory = new ProxyFactory(bean);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice((MethodInterceptor) invocation -> {
            Method targetMethod = AopUtils.getMostSpecificMethod(invocation.getMethod(), targetClass);
            if (!reactiveScheduledMethods.contains(targetMethod)
                    && !reactiveScheduledMethods.contains(invocation.getMethod())) {
                return invocation.proceed();
            }

            Object result = invocation.proceed();
            if (result == null) {
                throw new IllegalStateException("Reactive @Scheduled method returned null: " + targetMethod);
            }
            ReactiveAdapter adapter = adapters.getAdapter(targetMethod.getReturnType());
            return adapter.fromPublisher(new BlockingReactivePublisher<>(adapter.toPublisher(result)));
        });
        return proxyFactory.getProxy(bean.getClass().getClassLoader());
    }
}
