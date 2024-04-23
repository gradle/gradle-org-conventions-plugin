package com.gradle.enterprise.fixtures;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.gradle.api.Transformer;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import java.io.IOException;
import java.util.function.BiFunction;

abstract class ProxyProperty<T> implements Property<T> {
    static class ProxyPropertySerializer extends JsonSerializer<ProxyProperty> {
        @Override
        public void serialize(ProxyProperty property, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            Object value = property.getOrNull();
            if (value == null) {
                gen.writeNull();
            } else if (value instanceof Boolean) {
                gen.writeBoolean((Boolean) value);
            } else {
                gen.writeString(value.toString());
            }
        }
    }

    @Override
    public void set(Provider<? extends T> provider) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property<T> value(@javax.annotation.Nullable T value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property<T> value(Provider<? extends T> provider) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property<T> convention(@javax.annotation.Nullable T value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property<T> convention(Provider<? extends T> provider) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void finalizeValue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void finalizeValueOnRead() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void disallowChanges() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void disallowUnsafeRead() {
        throw new UnsupportedOperationException();
    }

    @javax.annotation.Nullable
    @Override
    public T getOrNull() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T get() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T getOrElse(T defaultValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S> Provider<S> map(Transformer<? extends S, ? super T> transformer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S> Provider<S> flatMap(Transformer<? extends Provider<? extends S>, ? super T> transformer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPresent() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Provider<T> orElse(T value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Provider<T> orElse(Provider<? extends T> provider) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Provider<T> forUseAtConfigurationTime() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U, R> Provider<R> zip(Provider<U> provider, BiFunction<? super T, ? super U, ? extends R> biFunction) {
        throw new UnsupportedOperationException();
    }
}
