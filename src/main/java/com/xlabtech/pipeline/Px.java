package com.xlabtech.pipeline;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Px<R> {

    private final Optional<Function> initFunction;
    private final ObservableFunction obsAfter;
    private final ObservableFunction obsBefore;
    private final Consumer complete;
    private final Function error;

    public Px() {
        this.initFunction = Optional.empty();
        this.obsAfter = new ObservableFunction();
        this.obsBefore = new ObservableFunction();
        this.complete = (x) -> {
        };
        this.error = (x) -> x;
    }

    private Px(Function initFunction, ObservableFunction obsAfter, ObservableFunction obsBefore, Consumer complete, Function error) {
        this.initFunction = Optional.ofNullable(initFunction);
        this.obsAfter = obsAfter;
        this.obsBefore = obsBefore;
        this.complete = complete;
        this.error = error;
    }

    private Px(Optional<Function> function, ObservableFunction obsAfter, ObservableFunction obsBefore, Consumer complete, Function error) {
        this.initFunction = function;
        this.obsAfter = obsAfter;
        this.obsBefore = obsBefore;
        this.complete = complete;
        this.error = error;
    }

    /**
     * Reduce array functions to function chain others when this is completed.
     *
     * @param functions array functions to reduce
     * @param <T>       type input functions
     * @param <U>       type returned functions
     * @return copy pipelne object
     */
    public <T, U> Px<R> chain(Function<T, U>... functions) {
        Optional<Function> function = Arrays.stream(functions).map(f -> obsBefore.andThen(f))
                .map(f -> f.andThen(obsAfter))
                .reduce((acc, idd) -> acc.andThen(idd));

        if (this.initFunction.isPresent())
            return new Px(this.initFunction.get().andThen(function.get()), this.obsAfter, this.obsBefore, this.complete, this.error);
        else return new Px(function, this.obsAfter, this.obsBefore, this.complete, this.error);
    }


    /**
     * Invoke all functions in parallel execution and recover response in order list.
     *
     * @param functions array functions to reduce
     * @param <T>       type input functions
     * @param <U>       type returned functions
     * @return copy pipelne object
     */
    public <T, U> Px<R> chainConcat(Function<T, U>... functions) {
        return chain(((input) -> (getAssyncFunctions(input, functions)).collect(Collectors.toList())));
    }

    /**
     * Invoke all functions in parallel execution and recover response in order collector.
     *
     * @param collector collector to cast response
     * @param functions array functions to reduce
     * @param <T>       type input functions
     * @param <U>       type returned functions
     * @return copy pipelne object
     */
    public <T, U> Px<R> chainConcatWithCustomCollector(Collector collector, Function<T, U>... functions) {
        return chain(((input) -> (getAssyncFunctions(input, functions)).collect(collector)));
    }

    /**
     * Invoke function if predicate is true
     *
     * @param filter    to validate if execute function
     * @param functions array functions to reduce
     * @param <T>       type input functions
     * @param <U>       type returned functions
     * @return copy pipelne object
     */
    public <T, U> Px<R> chainIf(Predicate filter, Function<T, U>... functions) {
        return chain((input) -> Arrays.stream(functions).filter((x) -> filter.test(input))
                .reduce((acc, idd) -> reduce(acc, idd)).map(f -> f.apply((T) input)).orElse((U) input));
    }

    /**
     * Invoke Px if predicate is true
     *
     * @param filter to validate if execute function
     * @param pipes  array Px to reduce and concat
     * @param <T>    type input functions
     * @param <U>    type returned functions
     * @return copy pipelne object
     */
    public <T, U> Px<R> chainIf(Predicate filter, Px... pipes) {
        return chain((input) -> Arrays.stream(pipes).filter((x) -> filter.test(input))
                .reduce((acc, idd) -> acc.andThen(idd)).map(p -> p.apply(input)).orElse(input));
    }

    /**
     * Invoke Px if predicate is true or invoke second Px if predicate is false
     *
     * @param filter    to validate if execute function
     * @param pipeTrue  Px to invoke if true
     * @param pipeFalse Px to invoke if false
     * @param <T>       type input functions
     * @param <U>       type returned functions
     * @return copy pipelne object
     */
    public <T, U> Px<R> chainIf(Predicate filter, Px pipeTrue, Px pipeFalse) {
        return chain((input) -> {
            if (filter.test(input))
                return pipeTrue.apply(input);
            return pipeFalse.apply(input);
        });
    }

    /**
     * Add log subscribers with println time to execute and input/output all functions.
     *
     * @return copy pipelne object
     */
    public Px<R> debugEnabled() {
        return subscribeAfter(x -> System.out.println("finish function time: " + new Date().getTime() + " with output: " + x.toString()))
                .subscribeBefore(x -> System.out.println("init function time: " + new Date().getTime() + " with input: " + x.toString()))
                .subscribeError(x -> {
                    ((Exception) x).printStackTrace();
                    return x;
                });
    }

    /**
     * Execute sync pipeline
     *
     * @return
     */
    public PipelineSync<R> sync() {
        return new PipelineSync<R>();
    }

    /**
     * Add subscribe to execute after all functions
     *
     * @param after subscribe to run after all functions
     * @param <T>
     * @return
     */
    public <T> Px<R> subscribeAfter(Consumer<T>... after) {
        Arrays.stream(after).forEach(f -> this.obsAfter.addObserver((o, arg) -> f.accept((T) arg)));
        return new Px(this.initFunction, this.obsAfter, this.obsBefore, this.complete, this.error);
    }

    /**
     * Add subscribe to execute before all functions
     *
     * @param before subscribe to run after all functions
     * @param <T>
     * @return
     */
    public <T> Px<R> subscribeBefore(Consumer<T>... before) {
        Arrays.stream(before).forEach(f -> this.obsBefore.addObserver((o, arg) -> f.accept((T) arg)));
        return new Px(this.initFunction, this.obsAfter, this.obsBefore, this.complete, this.error);
    }

    /**
     * Add subscribe to execute if pipeline throw error
     *
     * @param error
     * @param <T>
     * @param <U>
     * @return
     */
    public <T, U> Px<R> subscribeError(Function<T, U> error) {
        return new Px(this.initFunction, this.obsAfter, this.obsBefore, this.complete, error);
    }

    /**
     * Add subscribe to execute when complete pipeline
     *
     * @param complete
     * @param <T>
     * @return
     */
    public <T> Px<R> subscribeResult(Consumer<T> complete) {
        return new Px(this.initFunction, this.obsAfter, this.obsBefore, complete, this.error);
    }

    /**
     * Execute same time all pipes pass in param with specific collector
     *
     * @param collector to cast results pipes
     * @param pipes     chain of pipeline
     * @return
     */
    public Px<R> branchWithCustomCollector(Collector collector, Px... pipes) {
        Objects.requireNonNull(pipes);
        return chain((input) -> getAssyncPx(input, pipes).collect(collector));
    }

    /**
     * Execute same time all pipes pass in param
     *
     * @param pipes chain of pipeline
     * @return
     */
    public Px<R> branch(Px... pipes) {
        Objects.requireNonNull(pipes);
        return chain((input) -> getAssyncPx(input, pipes).collect(Collectors.toList()));
    }

    /**
     * Execute chain of pipes when these is completed
     *
     * @param pipes chain of pipe
     * @return
     */
    public Px<R> concat(Px... pipes) {
        Objects.requireNonNull(pipes);
        return chain((input) -> Arrays.stream(pipes).reduce((acc, idd) -> acc.andThen(idd)).map(p -> p.apply(input)).orElse((R) input));
    }

    /**
     * @param pipeAfter pipe to execute after this.
     * @return
     */
    public Px<R> andThen(Px pipeAfter) {
        Objects.requireNonNull(pipeAfter);
        return chain((input) -> pipeAfter.apply(input));
    }

    /**
     * @param pipeBefore pipe to execute before this
     * @return
     */
    public Px<R> compose(Px pipeBefore) {
        Objects.requireNonNull(pipeBefore);
        return new Px((input) -> apply(pipeBefore.apply(input)), this.obsAfter, this.obsBefore, this.complete, this.error);
    }

    /**
     * Convert this pipeline to Observer java.
     *
     * @return
     */
    public Observer toObserver() {
        return (o, arg) -> this.sync().execute(arg);
    }

    public void addPxInObservable(Observable observable) {
        observable.addObserver(toObserver());
    }

    /**
     * Execute assync pipeline
     *
     * @param param to input frist function in pipeline.
     */
    public void execute(Object param) {
        CompletableFuture.runAsync(() -> this.complete.accept(apply(param)))
                .exceptionally(this.error);
    }

    private R apply(Object param) {
        return (R) initFunction.orElse(x -> x.toString()).apply(param);
    }

    private <T, U> Function<T, U> reduce(Function acc, Function idd) {
        return acc.andThen(idd);
    }

    private Stream getAssyncFunctions(Object x, Function... functions) {
        return Arrays.stream(functions).parallel().map(f -> f.apply(x));
    }

    private Stream getAssyncPx(Object x, Px... pipes) {
        return Arrays.stream(pipes).parallel().map(p -> p.apply(x));
    }

    public class PipelineSync<T> {

        /**
         * Execute sync pipeline.
         *
         * @param param
         * @return
         */
        public T execute(Object param) {
            return (T) initFunction.orElse(x -> x.toString()).apply(param);
        }

    }
}