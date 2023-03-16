package it.sephiroth.android.rxjava3.extensions.operators;

import java.util.function.Function;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.internal.observers.BasicFuseableObserver;

public class MapNotNullObserver<T, R> extends BasicFuseableObserver<T, R> {
    private final Function<T, ? extends R> mapper;

    MapNotNullObserver(Observer<R> downstream, java.util.function.Function<T, ? extends R> mapper) {
        super(downstream);
        this.mapper = mapper;
    }


    @Override
    public void onNext(@NonNull T t) {
        if (done) {
            return;
        }

        if (sourceMode != NONE) {
            downstream.onNext(null);
            return;
        }

        R result;

        try {
            result = mapper.apply(t);
        } catch (Throwable ex) {
            fail(ex);
            return;
        }

        if (null != result) {
            downstream.onNext(result);
        }
    }

    @Override
    public int requestFusion(int mode) {
        return transitiveBoundaryFusion(mode);
    }

    @Override
    public @Nullable R poll() throws Throwable {
        while (true) {
            T item = qd.poll();
            if (null == item) return null;

            R result = mapper.apply(item);
            if (null != result) {
                return result;
            }
        }
    }
}
