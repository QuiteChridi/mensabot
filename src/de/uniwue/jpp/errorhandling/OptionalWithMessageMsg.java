package de.uniwue.jpp.errorhandling;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class OptionalWithMessageMsg<T> implements OptionalWithMessage<T>{
    private final String message;

    OptionalWithMessageMsg(String message){
        this.message = message;
    }

    @Override
    public boolean isPresent() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public T get() {
        throw new NoSuchElementException();
    }

    @Override
    public T orElse(T def) {
        return def;
    }

    @Override
    public T orElseGet(Supplier<? extends T> supplier) {
        return supplier.get();
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public <S> OptionalWithMessage<S> map(Function<T, S> f) {
        return  OptionalWithMessage.ofMsg(message);
    }

    @Override
    public <S> OptionalWithMessage<S> flatMap(Function<T, OptionalWithMessage<S>> f) {
        return  OptionalWithMessage.ofMsg(message);
    }

    @Override
    public Optional<String> consume(Consumer<T> c) {
        return Optional.of(message);
    }

    @Override
    public Optional<String> tryToConsume(Function<T, Optional<String>> c) {
        return Optional.of(message);
    }


}
