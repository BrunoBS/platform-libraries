package br.com.portalmanager.platform.library.testing.fixture.factory;

import br.com.portalmanager.platform.library.testing.fixture.builder.TestDataBuilder;
import br.com.portalmanager.platform.library.testing.fixture.factory.TestDataFactory;

import java.util.Objects;
import java.util.function.Consumer;

public abstract class AbstractTestDataFactory<
        T,
        B extends TestDataBuilder<T>>
        implements TestDataFactory<T> {

    protected abstract B builder();

    @Override
    public T valid() {
        return create(ignored -> {});
    }

    protected final T create(Consumer<? super B> customization) {
        Objects.requireNonNull(customization, "Customization must not be null");

        B dataBuilder = Objects.requireNonNull(
                builder(),
                "builder() must not return null"
        );
        customization.accept(dataBuilder);
        return dataBuilder.build();
    }
}
