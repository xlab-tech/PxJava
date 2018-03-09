package com.xlabtech.pipeline;

import java.util.Observable;
import java.util.function.Function;

public class ObservableFunction<T,R> extends Observable implements Function<T,R> {

    @Override
    public R apply(T o) {
        setChanged();
        notifyObservers(o);
        return (R) o;
    }
}
