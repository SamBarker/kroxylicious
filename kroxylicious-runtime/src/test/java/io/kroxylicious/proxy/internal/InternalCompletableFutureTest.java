/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.internal;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoop;

import static org.assertj.core.api.Assertions.assertThat;

class InternalCompletableFutureTest {

    private static EventLoop executor;

    @BeforeAll
    static void beforeAll() {
        executor = new DefaultEventLoop(Executors.newSingleThreadExecutor());
    }

    @AfterAll
    static void afterAll() {
        executor.shutdownGracefully(0, 0, TimeUnit.SECONDS);
    }

    @Test
    void asyncChainingMethodExecutesOnThreadOfExecutor() throws Exception {
        var threadOfExecutor = executor.submit(Thread::currentThread).get();
        var future = InternalCompletableFuture.completedFuture(executor, null);

        var actualThread = new AtomicReference<Thread>();
        var result = future.thenAcceptAsync((u) -> {
            assertThat(actualThread).hasValue(null);
            actualThread.set(Thread.currentThread());
        });
        result.join();

        assertThat(result).isCompleted();
        assertThat(actualThread).hasValue(threadOfExecutor);

    }

    static Stream<Arguments> allChainingMethods() {
        var other = CompletableFuture.<Void> completedFuture(null);
        var completed = CompletableFuture.<Void> completedFuture(null);
        return Stream.of(
                Arguments.of("thenAccept", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.thenAccept(u -> {
                    assertThat(executor.inEventLoop()).isTrue();
                })),
                Arguments.of("thenAcceptAsync", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.thenAcceptAsync(u -> {
                    assertThat(executor.inEventLoop()).isTrue();

                })),
                Arguments.of("thenAcceptAsync(E)", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.thenAcceptAsync(u -> {
                    assertThat(executor.inEventLoop()).isTrue();
                }, e)),

                Arguments.of("thenApply", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.thenApply(u -> {
                    assertThat(executor.inEventLoop()).isTrue();
                    return u;
                })),
                Arguments.of("thenApplyAsync", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.thenApplyAsync(u -> {
                    assertThat(executor.inEventLoop()).isTrue();
                    return u;
                })),
                Arguments.of("thenApplyAsync(E)", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.thenApplyAsync(u -> {
                    assertThat(executor.inEventLoop()).isTrue();
                    return u;
                }, e)),

                Arguments.of("thenCombine", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.thenCombine(other, (u1, u2) -> {
                    assertThat(executor.inEventLoop()).isTrue();
                    return u1;
                })),
                Arguments.of("thenCombineAsync",
                        (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.thenCombineAsync(other, (u1, u2) -> {
                            assertThat(executor.inEventLoop()).isTrue();
                            return u1;
                        })),
                Arguments.of("thenCombineAsync(E)",
                        (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.thenCombineAsync(other, (u1, u2) -> {
                            assertThat(executor.inEventLoop()).isTrue();
                            return u1;
                        }, e)),

                Arguments.of("thenCompose", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.thenCompose(u -> {
                    assertThat(executor.inEventLoop()).isTrue();
                    return completed;
                })),
                Arguments.of("thenComposeAsync", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.thenComposeAsync(u -> {
                    assertThat(executor.inEventLoop()).isTrue();
                    return completed;
                })),
                Arguments.of("thenComposeAsync(E)", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.thenComposeAsync(u -> {
                    assertThat(executor.inEventLoop()).isTrue();
                    return completed;
                }, e)),

                Arguments.of("thenRun", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.thenRun(() -> {
                    assertThat(executor.inEventLoop()).isTrue();
                })),
                Arguments.of("thenRunAsync", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.thenRunAsync(() -> {
                    assertThat(executor.inEventLoop()).isTrue();
                })),
                Arguments.of("thenRunAsync(E)", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.thenRunAsync(() -> {
                    assertThat(executor.inEventLoop()).isTrue();
                }, e)),

                Arguments.of("handle", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.handle((u, t) -> {
                    assertThat(executor.inEventLoop()).isTrue();
                    return u;
                })),
                Arguments.of("handleAsync", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.handleAsync((u, t) -> {
                    assertThat(executor.inEventLoop()).isTrue();
                    return u;
                })),
                Arguments.of("handleAsync(E)", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.handleAsync((u, t) -> {
                    assertThat(executor.inEventLoop()).isTrue();
                    return u;
                }, e)),

                Arguments.of("whenComplete", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.whenComplete((u, t) -> {
                    assertThat(executor.inEventLoop()).isTrue();
                })),
                Arguments.of("whenCompleteAsync", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.whenCompleteAsync((u, t) -> {
                    assertThat(executor.inEventLoop()).isTrue();
                })),
                Arguments.of("whenCompleteAsync(E)", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.whenCompleteAsync((u, t) -> {
                    assertThat(executor.inEventLoop()).isTrue();
                }, e)),

                Arguments.of("exceptionally", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.exceptionally(t -> {
                    assertThat(executor.inEventLoop()).isTrue();
                    return null;
                })),
                Arguments.of("exceptionallyAsync", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.exceptionallyAsync(t -> {
                    assertThat(executor.inEventLoop()).isTrue();
                    return null;
                })),
                Arguments.of("exceptionallyAsync(E)", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.exceptionallyAsync(t -> {
                    assertThat(executor.inEventLoop()).isTrue();
                    return null;
                }, e)),

                Arguments.of("exceptionallyCompose",
                        (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.exceptionallyCompose(t -> {
                            assertThat(executor.inEventLoop()).isTrue();
                            return completed;
                        })),
                Arguments.of("exceptionallyComposeAsync",
                        (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.exceptionallyComposeAsync(t -> {
                            assertThat(executor.inEventLoop()).isTrue();
                            return completed;
                        })),
                Arguments.of("exceptionallyComposeAsync(E)",
                        (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.exceptionallyComposeAsync(t -> {
                            assertThat(executor.inEventLoop()).isTrue();
                            return completed;
                        }, e)),

                Arguments.of("acceptEither", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.acceptEither(other, u -> {
                    assertThat(executor.inEventLoop()).isTrue();
                })),
                Arguments.of("acceptEitherAsync", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.acceptEitherAsync(other, u -> {
                    assertThat(executor.inEventLoop()).isTrue();
                })),
                Arguments.of("acceptEitherAsync(E)", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.acceptEitherAsync(other, u -> {
                    assertThat(executor.inEventLoop()).isTrue();
                }, e)),

                Arguments.of("applyToEither", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.applyToEither(other, u -> {
                    assertThat(executor.inEventLoop()).isTrue();
                    return u;
                })),
                Arguments.of("applyToEitherAsync", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.applyToEitherAsync(other, u -> {
                    assertThat(executor.inEventLoop()).isTrue();
                    return u;
                })),
                Arguments.of("applyToEitherAsync(E)",
                        (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.applyToEitherAsync(other, u -> {
                            assertThat(executor.inEventLoop()).isTrue();
                            return u;
                        }, e)),

                Arguments.of("runAfterEither", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.runAfterEither(other, () -> {
                    assertThat(executor.inEventLoop()).isTrue();
                })),
                Arguments.of("runAfterEitherAsync", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.runAfterEitherAsync(other, () -> {
                    assertThat(executor.inEventLoop()).isTrue();
                })),
                Arguments.of("runAfterEitherAsync(E)", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.runAfterEitherAsync(other, () -> {
                    assertThat(executor.inEventLoop()).isTrue();
                }, e)),

                Arguments.of("theAcceptBoth", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.thenAcceptBoth(other, (u1, u2) -> {
                    assertThat(executor.inEventLoop()).isTrue();
                })),
                Arguments.of("thenAcceptBothAsync",
                        (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.thenAcceptBothAsync(other, (u1, u2) -> {
                            assertThat(executor.inEventLoop()).isTrue();
                        })),
                Arguments.of("thenAcceptBothAsync(E)",
                        (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.thenAcceptBothAsync(other, (u1, u2) -> {
                            assertThat(executor.inEventLoop()).isTrue();
                        }, e)),

                Arguments.of("runAfterBoth", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.runAfterBoth(other, () -> {
                    assertThat(executor.inEventLoop()).isTrue();
                })),
                Arguments.of("runAfterBothAsync", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.runAfterBothAsync(other, () -> {
                    assertThat(executor.inEventLoop()).isTrue();
                })),
                Arguments.of("runAfterBothAsync(E)", (BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>>) (s, e) -> s.runAfterBothAsync(other, () -> {
                    assertThat(executor.inEventLoop()).isTrue();
                }, e)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allChainingMethods")
    void chainedWorkIsExecutedOnEventLoop(String name, BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>> func) {
        var future = new InternalCompletableFuture<Void>(executor);
        var stage = future.minimalCompletionStage();
        var result = func.apply(stage, executor);
        assertThat(result.getClass()).isAssignableTo(InternalCompletionStage.class);
        CompletableFuture<Void> future1 = toFuture(result);
        future.complete(null);
        assertThat(future1).succeedsWithin(2, TimeUnit.SECONDS);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allChainingMethods")
    void chainedWorkIsExecutedOnEventLoopFuture(String name, BiFunction<CompletionStage<Void>, Executor, CompletionStage<Void>> func) {
        var future = new InternalCompletableFuture<Void>(executor);
        var result = func.apply(future, executor);
        assertThat(result.getClass()).isAssignableTo(InternalCompletableFuture.class);
        CompletableFuture<Void> future1 = toFuture(result);
        future.complete(null);
        assertThat(future1).succeedsWithin(2, TimeUnit.SECONDS);
    }

    private static CompletableFuture<Void> toFuture(CompletionStage<Void> result) {
        CompletableFuture<Void> future1 = new CompletableFuture<>();
        result.whenComplete((unused, throwable) -> {
            if (throwable != null) {
                future1.completeExceptionally(throwable);
            }
            else {
                future1.complete(null);
            }
        });
        return future1;
    }
}
