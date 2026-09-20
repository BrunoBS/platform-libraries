package com.empresa.platform.authorization.resource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.data.jpa.repository.query.BeanFactoryQueryRewriterProvider;
import org.springframework.data.jpa.repository.query.QueryRewriterProvider;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;

/**
 * Installs the platform QueryRewriterProvider into Spring Data JPA repository
 * factories without requiring repository methods to declare queryRewriter.
 */
public class ResourceVisibilityRepositoryFactoryBeanPostProcessor
        implements BeanPostProcessor, BeanFactoryAware {

    private final ResourceVisibilityNativeQueryRewriter visibilityRewriter;
    private BeanFactory beanFactory;

    public ResourceVisibilityRepositoryFactoryBeanPostProcessor(
            ResourceVisibilityNativeQueryRewriter visibilityRewriter) {
        this.visibilityRewriter = visibilityRewriter;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (bean instanceof JpaRepositoryFactoryBean<?, ?, ?> factoryBean) {
            QueryRewriterProvider delegate = new BeanFactoryQueryRewriterProvider(beanFactory);
            QueryRewriterProvider provider =
                    new ResourceVisibilityQueryRewriterProvider(delegate, visibilityRewriter);

            factoryBean.addRepositoryFactoryCustomizer(repositoryFactory -> {
                if (repositoryFactory instanceof JpaRepositoryFactory jpaRepositoryFactory) {
                    jpaRepositoryFactory.setQueryRewriterProvider(provider);
                }
            });
        }
        return bean;
    }
}
