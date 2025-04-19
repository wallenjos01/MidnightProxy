package org.wallentines.mdproxy.requirement;

import org.jetbrains.annotations.NotNull;
import org.wallentines.mdcfg.serializer.ObjectSerializer;
import org.wallentines.mdcfg.serializer.SerializeContext;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.mdproxy.ConnectionContext;
import org.wallentines.midnightlib.math.Range;
import org.wallentines.mdcfg.registry.Identifier;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;

public class DateCheck implements ConnectionCheck {

    private final ZoneId timeZone;
    private final Range<Integer> second;
    private final Range<Integer> minute;
    private final Range<Integer> hour;
    private final Range<Integer> day;
    private final Range<Integer> month;
    private final Range<Integer> year;

    public DateCheck(ZoneId timeZone, Range<Integer> second, Range<Integer> minute, Range<Integer> hour, Range<Integer> day, Range<Integer> month, Range<Integer> year) {
        this.timeZone = timeZone;
        this.second = second;
        this.minute = minute;
        this.hour = hour;
        this.day = day;
        this.month = month;
        this.year = year;
    }

    @Override
    public boolean requiresAuth() {
        return false;
    }

    @Override
    public @NotNull Collection<Identifier> getRequiredCookies() {
        return Collections.emptySet();
    }

    @Override
    public boolean check(ConnectionContext ctx) {

        ZonedDateTime zdt = Instant.now().atZone(timeZone);

        return second.isWithin(zdt.getSecond()) &&
                minute.isWithin(zdt.getMinute()) &&
                hour.isWithin(zdt.getHour()) &&
                day.isWithin(zdt.getDayOfMonth()) &&
                month.isWithin(zdt.getMonthValue()) &&
                year.isWithin(zdt.getYear());
    }

    @Override
    public Type type() {
        return TYPE;
    }

    public static final Type TYPE = new Type();

    public static class Type implements ConnectionCheckType<DateCheck> {

        @Override
        public Serializer<DateCheck> serializer() {
            return SERIALIZER;
        }
    }


    public static final Serializer<ZoneId> ZONE_SERIALIZER = new Serializer<>() {
        @Override
        public <O> SerializeResult<O> serialize(SerializeContext<O> ctx, ZoneId zoneId) {
            return SerializeResult.success(ctx.toString(zoneId.getId()));
        }

        @Override
        public <O> SerializeResult<ZoneId> deserialize(SerializeContext<O> ctx, O o) {
            return ctx.asString(o).map(str -> {
                try {
                    return SerializeResult.success(ZoneId.of(str));
                } catch (DateTimeException ex) {
                    return SerializeResult.failure("Unable to parse zone " + str + "!");
                }
            });
        }
    };

    public static final Serializer<DateCheck> SERIALIZER = ObjectSerializer.create(
            ZONE_SERIALIZER.<DateCheck>entry("time_zone", dc -> dc.timeZone).orElse(ZoneId.systemDefault()),
            Range.INTEGER.<DateCheck>entry("second", dc -> dc.second).orElse(Range.all()),
            Range.INTEGER.<DateCheck>entry("minute", dc -> dc.minute).orElse(Range.all()),
            Range.INTEGER.<DateCheck>entry("hour", dc -> dc.hour).orElse(Range.all()),
            Range.INTEGER.<DateCheck>entry("day", dc -> dc.day).orElse(Range.all()),
            Range.INTEGER.<DateCheck>entry("month", dc -> dc.month).orElse(Range.all()),
            Range.INTEGER.<DateCheck>entry("year", dc -> dc.year).orElse(Range.all()),
            DateCheck::new
    );



}
