/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.internal;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.netty.channel.DefaultEventLoop;
import io.netty.util.concurrent.EventExecutor;

import static org.junit.jupiter.params.provider.Arguments.argumentSet;

class InternalCompletionStageTest {

    private static EventExecutor eventExecutor;

    @BeforeAll
    static void beforeAll() {
        eventExecutor = new DefaultEventLoop(Executors.newSingleThreadExecutor());
    }

    @AfterAll
    static void afterAll() {
        eventExecutor.shutdownGracefully(1, 1, TimeUnit.MILLISECONDS);
    }

    @ParameterizedTest
    @MethodSource("allChainingMethods")
    void shouldWrapCompletionStage(Function<CompletionStage<Void>, CompletionStage<Void>> testFunc) {
        // Given
        CompletionStage<Void> internalCompletionStage = new InternalCompletionStage<>(CompletableFuture.completedStage(null));

        // When
        CompletionStage<Void> result = testFunc.apply(internalCompletionStage);

        // Then
        if (!(result instanceof InternalCompletionStage<Void>)) {
            // Note we can't do the far nicer assertion
            // assertThat(result).isInstanceOf(InternalCompletionStage.class);
            // As assertJ calls `toCompletableFuture` when it gets a completion stage, and thus we loose actual instance.
            Assertions.fail("result was not an " + InternalCompletionStage.class.getSimpleName());
        }
    }

    static Stream<Arguments> allChainingMethods() {
        var other = CompletableFuture.<Void> completedFuture(null);
        return Stream.of(
                argumentSet("thenAccept",
                        (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.thenAccept(unused -> {
                        })),
                argumentSet("thenAcceptAsync",
                        (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.thenAcceptAsync(unused -> {
                        })),
                argumentSet("thenAcceptAsync(E)",
                        (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.thenAcceptAsync(unused -> {
                        }, eventExecutor)),

                argumentSet("thenApply", (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.thenApply(unused -> null)),
                argumentSet("thenApplyAsync", (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.thenApplyAsync(unused -> null)),
                argumentSet("thenApplyAsync(E)", (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.thenApplyAsync(unused -> null, eventExecutor)),

                argumentSet("thenCombine", (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.thenCombine(other, (unused, unused2) -> null)),
                argumentSet("thenCombineAsync",
                        (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.thenCombineAsync(other, (unused, unused2) -> null)),
                argumentSet("thenCombineAsync(E)",
                        (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.thenCombineAsync(other, (unused, unused2) -> null, eventExecutor)),

                argumentSet("thenCompose", (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.thenCompose(unused -> null)),
                argumentSet("thenComposeAsync", (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.thenComposeAsync(
                        unused -> null)),
                argumentSet("thenComposeAsync(E)", (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.thenComposeAsync(
                        unused -> null, eventExecutor)),

                argumentSet("thenRun",
                        (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.thenRun(() -> {
                        })),
                argumentSet("thenRunAsync",
                        (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.thenRunAsync(() -> {
                        })),
                argumentSet("thenRunAsync(E)",
                        (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.thenRunAsync(() -> {
                        }, eventExecutor)),

                argumentSet("handle",
                        (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.handle((unused, throwable) -> null)),
                argumentSet("handleAsync", (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.handleAsync((unused, throwable) -> null)),
                argumentSet("handleAsync(E)", (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.handleAsync(
                        (unused, throwable) -> null, eventExecutor)),

                argumentSet("whenComplete",
                        (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.whenComplete((unused, throwable) -> {
                        })),
                argumentSet("whenCompleteAsync",
                        (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.whenCompleteAsync((unused, throwable) -> {
                        })),
                argumentSet("whenCompleteAsync(E)",
                        (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.whenCompleteAsync((unused, throwable) -> {
                                },
                                eventExecutor)),

                argumentSet("acceptEither", (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.acceptEither(other,
                        unused -> {
                        })),
                argumentSet("acceptEitherAsync",
                        (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.acceptEitherAsync(other,
                                unused -> {
                                })),
                argumentSet("acceptEitherAsync(E)",
                        (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.acceptEitherAsync(other,
                                unused -> {
                                }, eventExecutor)),

                argumentSet("applyToEither", (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.applyToEither(other,
                        unused -> null)),
                argumentSet("applyToEitherAsync",
                        (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.applyToEitherAsync(other,
                                unused -> null)),
                argumentSet("applyToEitherAsync(E)",
                        (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.applyToEitherAsync(other,
                                unused -> null, eventExecutor)),

                argumentSet("runAfterEither", (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.runAfterEither(other,
                        () -> {
                        })),
                argumentSet("runAfterEitherAsync",
                        (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.runAfterEitherAsync(other,
                                () -> {
                                })),
                argumentSet("runAfterEitherAsync(E)",
                        (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.runAfterEitherAsync(other,
                                () -> {
                                }, eventExecutor)),

                argumentSet("theAcceptBoth",
                        (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.thenAcceptBoth(other,
                                (unused, unused2) -> {
                                })),
                argumentSet("thenAcceptBothAsync",
                        (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.thenAcceptBothAsync(other,
                                (unused, unused2) -> {
                                })),
                argumentSet("thenAcceptBothAsync(E)",
                        (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.thenAcceptBothAsync(other,
                                (unused, unused2) -> {
                                }, eventExecutor)),

                argumentSet("runAfterBoth", (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.runAfterBoth(other,
                        () -> {
                        })),
                argumentSet("runAfterBothAsync",
                        (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.runAfterBothAsync(other,
                                () -> {
                                })),
                argumentSet("runAfterBothAsync(E)",
                        (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.runAfterBothAsync(other,
                                () -> {
                                }, eventExecutor)),

                argumentSet("exceptionally", (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.exceptionally(throwable -> null)),
                argumentSet("exceptionallyAsync", (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.exceptionallyAsync(throwable -> null)),
                argumentSet("exceptionallyAsync(E)",
                        (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.exceptionallyAsync(throwable -> null, eventExecutor)),

                argumentSet("exceptionallyCompose",
                        (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.exceptionallyCompose(throwable -> null)),
                argumentSet("exceptionallyComposeAsync",
                        (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.exceptionallyComposeAsync(throwable -> null, eventExecutor)),
                argumentSet("exceptionallyComposeAsync(E)",
                        (Function<CompletionStage<Void>, CompletionStage<Void>>) s -> s.exceptionallyComposeAsync(throwable -> null, eventExecutor)));
    }

}