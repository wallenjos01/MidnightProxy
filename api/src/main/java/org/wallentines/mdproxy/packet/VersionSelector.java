package org.wallentines.mdproxy.packet;

import org.wallentines.mcore.GameVersion;
import org.wallentines.mdcfg.Tuples;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

public class VersionSelector<T> {

    private final List<Tuples.T2<BiPredicate<GameVersion, ProtocolPhase>, T>> rules;
    private final T defaultValue;

    public VersionSelector(List<Tuples.T2<BiPredicate<GameVersion, ProtocolPhase>, T>> rules, T defaultValue) {
        this.rules = rules;
        this.defaultValue = defaultValue;
    }

    public T select(GameVersion version, ProtocolPhase phase) {
        for(Tuples.T2<BiPredicate<GameVersion, ProtocolPhase>, T> r : rules) {
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

        private final List<Tuples.T2<BiPredicate<GameVersion, ProtocolPhase>, T>> rules = new ArrayList<>();
        private T defaultValue;

        public Builder<T> add(BiPredicate<GameVersion, ProtocolPhase> predicate, T value) {
            rules.add(new Tuples.T2<>(predicate, value));
            return this;
        }

        public Builder<T> beforeVersion(int protocol, int snapshotProtocol, T value) {
            return add(
                    (ver, phase) -> ver.isSnapshot()
                            && ver.getSnapshotVersion() < snapshotProtocol
                            || ver.getProtocolVersion() < protocol,
                    value);
        }

        public Builder<T> afterVersion(int protocol, int snapshotProtocol, T value) {
            return add(
                    (ver, phase) -> ver.isSnapshot()
                            && ver.getSnapshotVersion() >= snapshotProtocol
                            || ver.getProtocolVersion() >= protocol,
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
                    (ver, p) ->
                            (ver.isSnapshot()
                                    && ver.getSnapshotVersion() < snapshotProtocol
                                    || ver.getProtocolVersion() < protocol)
                            && p == phase,
                    value);
        }

        public Builder<T> afterVersionInPhase(int protocol, int snapshotProtocol, ProtocolPhase phase, T value) {
            return add(
                    (ver, p) ->
                            (ver.isSnapshot()
                                    && ver.getSnapshotVersion() >= snapshotProtocol
                                    || ver.getProtocolVersion() >= protocol)
                                    && p == phase,
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
