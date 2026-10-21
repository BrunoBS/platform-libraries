package br.com.portalmanager.platform.library.redis.internal;

import br.com.portalmanager.platform.library.redis.annotation.RedisNamespace;
import br.com.portalmanager.platform.library.redis.core.RedisStore;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.AutowireCandidateResolver;

/** Decorates the existing resolver so normal qualifiers, primary selection and lazy proxies survive. */
public final class RedisNamespaceResolver implements AutowireCandidateResolver, BeanFactoryAware {
    private final AutowireCandidateResolver delegate;
    private final Environment environment;
    private RedisNamespaceStores stores;

    public RedisNamespaceResolver(AutowireCandidateResolver delegate, Environment environment) {
        this.delegate = delegate;
        this.environment = environment;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        if (!(beanFactory instanceof ConfigurableListableBeanFactory factory)) {
            throw RedisErrors.configuration();
        }
        stores = new RedisNamespaceStores(factory, environment);
        if (delegate instanceof BeanFactoryAware aware) {
            aware.setBeanFactory(beanFactory);
        }
    }

    @Override
    public Object getSuggestedValue(DependencyDescriptor descriptor) {
        RedisNamespace annotation = descriptor.getAnnotation(RedisNamespace.class);
        if (annotation == null) return delegate.getSuggestedValue(descriptor);
        if (descriptor.getDependencyType() != RedisStore.class) throw RedisErrors.configuration();
        return stores.resolve(annotation);
    }

    @Override
    public boolean isAutowireCandidate(BeanDefinitionHolder holder, DependencyDescriptor descriptor) {
        return delegate.isAutowireCandidate(holder, descriptor);
    }

    @Override
    public boolean isRequired(DependencyDescriptor descriptor) {
        return delegate.isRequired(descriptor);
    }

    @Override
    public boolean hasQualifier(DependencyDescriptor descriptor) {
        return delegate.hasQualifier(descriptor);
    }

    @Override
    public String getSuggestedName(DependencyDescriptor descriptor) {
        return delegate.getSuggestedName(descriptor);
    }

    @Override
    public Object getLazyResolutionProxyIfNecessary(DependencyDescriptor descriptor, String name) {
        return delegate.getLazyResolutionProxyIfNecessary(descriptor, name);
    }

    @Override
    public Class<?> getLazyResolutionProxyClass(DependencyDescriptor descriptor, String name) {
        return delegate.getLazyResolutionProxyClass(descriptor, name);
    }

    @Override
    public AutowireCandidateResolver cloneIfNecessary() {
        return new RedisNamespaceResolver(delegate.cloneIfNecessary(), environment);
    }
}
