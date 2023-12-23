package de.uniwue.jpp.errorhandling;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class OptionalWithMessageVal<T> implements OptionalWithMessage<T>{
    private final T value;

    OptionalWithMessageVal(T value){
        this.value = value;
    }

    @Override
    public boolean isPresent() {
        return true;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public T orElse(T def) {
        return value;
    }

    @Override
    public T orElseGet(Supplier<? extends T> supplier) {
        return value;
    }

    @Override
    public String getMessage() {
        throw new NoSuchElementException();
    }

    @Override
    public <S> OptionalWithMessage<S> map(Function<T, S> f) {
        return OptionalWithMessage.of(f.apply(value));
    }

    @Override
    public <S> OptionalWithMessage<S> flatMap(Function<T, OptionalWithMessage<S>> f) {
        return f.apply(value);
    }

    @Override
    public Optional<String> consume(Consumer<T> c) {
        c.accept(value);
        return Optional.empty();
    }

    @Override
    public Optional<String> tryToConsume(Function<T, Optional<String>> c) {
        return c.apply(value);
    }

}
