package de.uniwue.jpp.errorhandling;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface OptionalWithMessage<T> {

    boolean isPresent();

    boolean isEmpty();

    T get();

    T orElse(T def);

    T orElseGet(Supplier<? extends T> supplier);

    String getMessage();

    <S> OptionalWithMessage<S> map(Function<T, S> f);

    <S> OptionalWithMessage<S> flatMap(Function<T, OptionalWithMessage<S>> f);

    Optional<String> consume(Consumer<T> c);

    Optional<String> tryToConsume(Function<T, Optional<String>> c);

    static <T> OptionalWithMessage<T> of(T val) {
        if(val == null){
            throw new NullPointerException();
        }
        return new OptionalWithMessageVal<T>(val);
    }

    static <T> OptionalWithMessage<T> ofMsg(String msg) {
        if(msg == null){
            throw new NullPointerException();
        }
        return new OptionalWithMessageMsg<T>(msg);
    }

    static <T> OptionalWithMessage<T> ofNullable(T val, String msg) {
        if(msg == null){
            throw new NullPointerException();
        }

        if(val == null){
            return new OptionalWithMessageMsg<T>(msg);
        }
        return new OptionalWithMessageVal<T>(val);
    }

    static <T> OptionalWithMessage<T> ofOptional(Optional<T> opt, String msg) {
        if(msg == null ||opt == null){
            throw new NullPointerException();
        }

        if(opt.isEmpty()){
            return new OptionalWithMessageMsg<T>(msg);
        }
        return new OptionalWithMessageVal<T>(opt.get());
    }

    static <T> OptionalWithMessage<List<T>> sequence(List<OptionalWithMessage<T>> list) {
        List<T> listToReturn = new ArrayList<>();
        StringBuilder errorMessage = new StringBuilder();
        boolean errorInList = false;

        for (OptionalWithMessage<T> o: list){
            if(o.isEmpty()){
                if(errorInList){
                    errorMessage.append(System.lineSeparator());
                }
                errorMessage.append(o.getMessage());
                errorInList = true;
            }
            else{
                listToReturn.add(o.get());
            }
        }
        if (errorInList){
            return OptionalWithMessage.ofMsg(errorMessage.toString());
        }
        return OptionalWithMessage.of(listToReturn);
    }
}
