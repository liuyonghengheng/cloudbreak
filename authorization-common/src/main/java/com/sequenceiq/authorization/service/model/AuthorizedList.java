package com.sequenceiq.authorization.service.model;

import java.util.Objects;

public class AuthorizedList<T> {

    private final T result;

    public AuthorizedList(T result) {
        this.result = result;
    }

    public T getResult() {
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthorizedList<?> that = (AuthorizedList<?>) o;
        return Objects.equals(result, that.result);
    }

    @Override
    public int hashCode() {
        return Objects.hash(result);
    }
}
