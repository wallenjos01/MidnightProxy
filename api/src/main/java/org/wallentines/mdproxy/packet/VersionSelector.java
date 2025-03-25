package org.wallentines.mdproxy.packet;

import org.wallentines.mdcfg.Tuples;
import org.wallentines.pseudonym.text.ProtocolContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

public class VersionSelector<T> {

    private final List<Tuples.T2<BiPredicate<Integer, ProtocolPhase>, T>> rules;
    private final T defaultValue;

    public VersionSelector(List<Tuples.T2<BiPredicate<Integer, ProtocolPhase>, T>> rules, T defaultValue) {
        this.rules = rules;
        this.defaultValue = defaultValue;
    }

    public T select(Integer version, ProtocolPhase phase) {
        for(Tuples.T2<BiPredicate<Integer, ProtocolPhase>, T> r : rules) {
            if(r.p1.test(version, phase)) {
                return r.p2;
            }
        }
        return defaultValue;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static class Builder<T> {

        private final List<Tuples.T2<BiPredicate<Integer, ProtocolPhase>, T>> rules = new ArrayList<>();
        private T defaultValue;

        public Builder<T> add(BiPredicate<Integer, ProtocolPhase> predicate, T value) {
            rules.add(new Tuples.T2<>(predicate, value));
            return this;
        }

        public Builder<T> beforeVersion(int protocol, int snapshotProtocol, T value) {
            return add(
                    (ver, phase) -> {
                        if(ver > ProtocolContext.RELEASE_MAX_VERSION) {
                            return ver - ProtocolContext.RELEASE_MAX_VERSION < snapshotProtocol;
                        }
                        return ver < protocol;
                    },
                    value);
        }

        public Builder<T> afterVersion(int protocol, int snapshotProtocol, T value) {
            return add(
                    (ver, phase) -> {
                        if(ver > ProtocolContext.RELEASE_MAX_VERSION) {
                            return ver - ProtocolContext.RELEASE_MAX_VERSION >= snapshotProtocol;
                        }
                        return ver >= protocol;
                    },
                    value);
        }

        public Builder<T> inPhase(ProtocolPhase phase, T value) {
            return add(
                    (ver, p) -> p == phase,
                    value
            );
        }

        public Builder<T> beforeVersionInPhase(int protocol, int snapshotProtocol, ProtocolPhase phase, T value) {
            return add(
                    (ver, p) -> {
                        if(p != phase) return false;
                        if(ver > ProtocolContext.RELEASE_MAX_VERSION) {
                            return ver - ProtocolContext.RELEASE_MAX_VERSION < snapshotProtocol;
                        }
                        return ver < protocol;
                    },
                    value);
        }

        public Builder<T> afterVersionInPhase(int protocol, int snapshotProtocol, ProtocolPhase phase, T value) {
            return add(
                    (ver, p) -> {
                        if(p != phase) return false;
                        if(ver > ProtocolContext.RELEASE_MAX_VERSION) {
                            return ver - ProtocolContext.RELEASE_MAX_VERSION >= snapshotProtocol;
                        }
                        return ver >= protocol;
                    },
                    value);
        }

        public Builder<T> orElse(T defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public VersionSelector<T> build() {
            return new VersionSelector<>(List.copyOf(rules), defaultValue);
        }

    }

}
