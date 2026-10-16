package br.com.portalmanager.platform.library.redis;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.library.redis.cache.PlatformRedisCacheManager;
import br.com.portalmanager.platform.library.testing.redis.annotation.WithRedis;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

@SpringBootTest(
        classes = RedisCacheBootstrapIT.Application.class,
        properties = {
            "spring.application.name=cache-bootstrap",
            "platform.redis.cache.enabled=true"
        })
@WithRedis
class RedisCacheBootstrapIT {
    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class Application {
        @Bean
        PlatformTransactionManager transactionManager() {
            return new LocalTransactions();
        }

        @Bean
        Queries queries() {
            return new Queries();
        }
    }

    public static class Queries {
        private int reads;

        @Cacheable("bootstrap")
        public Value find(String id) {
            reads++;
            return new Value(id);
        }

        @Transactional
        @CachePut(cacheNames = "bootstrap", key = "#id")
        public Value rollbackPut(String id) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new Value("must-not-be-cached");
        }

        @Transactional
        @CacheEvict(cacheNames = "bootstrap", key = "#id")
        public void rollbackEvict(String id) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }

        @Transactional
        @Cacheable("rollback")
        public Value rollback(String id) {
            reads++;
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new Value(id);
        }

        public int reads() {
            return reads;
        }
    }

    static class LocalTransactions extends AbstractPlatformTransactionManager {
        protected Object doGetTransaction() {
            return new Object();
        }

        protected void doBegin(Object transaction, TransactionDefinition definition) {}

        protected void doCommit(DefaultTransactionStatus status) {}

        protected void doRollback(DefaultTransactionStatus status) {}
    }

    public record Value(String id) {}

    @Autowired Queries queries;
    @Autowired CacheManager manager;

    @Test
    void dependencyAndOptInEnableStandardAnnotationsWithoutManualInfrastructure() {
        assertThat(manager).isInstanceOf(PlatformRedisCacheManager.class);
        assertThat(queries.find("42")).isEqualTo(new Value("42"));
        assertThat(queries.find("42")).isEqualTo(new Value("42"));
        assertThat(queries.reads()).isEqualTo(1);
        queries.rollbackPut("42");
        assertThat(manager.getCache("bootstrap").get("42", Value.class)).isEqualTo(new Value("42"));
        queries.rollbackEvict("42");
        assertThat(manager.getCache("bootstrap").get("42", Value.class)).isEqualTo(new Value("42"));
        queries.rollback("rolled-back");
        assertThat(manager.getCache("rollback").get("rolled-back")).isNull();
        queries.rollback("rolled-back");
        assertThat(queries.reads()).isEqualTo(3);
    }
}
