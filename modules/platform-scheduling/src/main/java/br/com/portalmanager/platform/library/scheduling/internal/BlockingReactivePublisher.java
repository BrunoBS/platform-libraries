package br.com.portalmanager.platform.library.scheduling.internal;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/** Makes a Spring reactive scheduled subscription complete on the scheduler thread. */
final class BlockingReactivePublisher<T> implements Publisher<T> {
    private final Publisher<T> delegate;

    BlockingReactivePublisher(Publisher<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void subscribe(Subscriber<? super T> downstream) {
        CountDownLatch finished = new CountDownLatch(1);
        AtomicBoolean terminated = new AtomicBoolean();
        AtomicReference<Subscription> upstream = new AtomicReference<>();

        try {
            delegate.subscribe(new Subscriber<>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    upstream.set(subscription);
                    downstream.onSubscribe(new Subscription() {
                        @Override
                        public void request(long count) {
                            subscription.request(count);
                        }

                        @Override
                        public void cancel() {
                            subscription.cancel();
                            if (terminated.compareAndSet(false, true)) {
                                finished.countDown();
                            }
                        }
                    });
                }

                @Override
                public void onNext(T value) {
                    downstream.onNext(value);
                }

                @Override
                public void onError(Throwable failure) {
                    if (terminated.compareAndSet(false, true)) {
                        try {
                            downstream.onError(failure);
                        } finally {
                            finished.countDown();
                        }
                    }
                }

                @Override
                public void onComplete() {
                    if (terminated.compareAndSet(false, true)) {
                        try {
                            downstream.onComplete();
                        } finally {
                            finished.countDown();
                        }
                    }
                }
            });
        } catch (RuntimeException failure) {
            if (terminated.compareAndSet(false, true)) {
                try {
                    downstream.onError(failure);
                } finally {
                    finished.countDown();
                }
            }
        }

        try {
            finished.await();
        } catch (InterruptedException interrupted) {
            Subscription subscription = upstream.get();
            if (subscription != null) {
                subscription.cancel();
            }
            if (terminated.compareAndSet(false, true)) {
                downstream.onError(interrupted);
                finished.countDown();
            }
            Thread.currentThread().interrupt();
        }
    }
}
