package org.wallentines.mdproxy.command;

import org.wallentines.mdcfg.ConfigSection;

import java.util.*;

public class ArgumentParser {

    private final Map<String, ArgumentType> typesByName = new HashMap<>();
    private final Map<Character, ArgumentType> typesByAlias = new HashMap<>();

    private ArgumentParser addArgument(ArgumentType t) {
        typesByName.put(t.name, t);
        if(t.hasAlias()) {
            typesByAlias.put(t.alias, t);
        }
        return this;
    }
    public ArgumentParser addOption(String name) {
        return addArgument(new ArgumentType(name, null, null, false));
    }

    public ArgumentParser addOption(String name, char alias) {
        return addArgument(new ArgumentType(name, null, alias, false));
    }

    public ArgumentParser addOption(String name, String defaultValue) {
        return addArgument(new ArgumentType(name, defaultValue, null, false));
    }

    public ArgumentParser addOption(String name, char alias, String defaultValue) {
        return addArgument(new ArgumentType(name, defaultValue, alias, false));
    }

    public ArgumentParser addFlag(String name) {
        return addArgument(new ArgumentType(name, null,null, true));
    }

    public ArgumentParser addFlag(String name, char alias) {
        return addArgument(new ArgumentType(name, null, alias, true));
    }

    public ParseResult parse(String[] args) {

        Parsed out = new Parsed();
        for(int i = 0 ; i < args.length ; i++) {

            String arg = args[i];
            ArgumentType type = null;
            if (arg.startsWith("--")) {

                String name = arg.substring(2);
                type = typesByName.get(name);
                if(type == null) {
                    return ParseResult.failure("Unrecognized option " + name);
                }

            } else if(arg.startsWith("-")) {

                String aliases = arg.substring(1);
                for(char c : aliases.toCharArray()) {
                    type = typesByAlias.get(c);
                    if(type == null) {
                        return ParseResult.failure("Unrecognized flag " + c);
                    }
                    if(aliases.length() > 1) {
                        if(!type.isFlag()) {
                            return ParseResult.failure("Option " + type.getName() + " requires a value!");
                        }
                        out.flags.add(type.name);
                    }
                }

            } else {

                out.positional.add(arg);
                continue;
            }

            if(type == null) {
                return ParseResult.failure("Unrecognized option " + arg);
            }

            if(type.isFlag()) {
                out.flags.add(type.name);
            } else {

                if(i + 1 == args.length) {
                    return ParseResult.failure("Option " + type.getName() + " requires a value!");
                }

                out.values.put(type.name, args[++i]);
            }
        }

        return ParseResult.success(out);

    }

    public static class ParseResult {

        Parsed output;
        String error;

        private ParseResult(Parsed output, String error) {
            this.output = output;
            this.error = error;
        }

        public static ParseResult success(Parsed output) {
            return new ParseResult(output, null);
        }

        public static ParseResult failure(String error) {
            return new ParseResult(null, error);
        }

        public Parsed getOutput() {
            return output;
        }

        public String getError() {
            return error;
        }

        public boolean isError() {
            return error != null;
        }

    }

    public class Parsed {

        private final Map<String, String> values;
        private final Set<String> flags;
        private final List<String> positional;

        private Parsed() {
            this.values = new HashMap<>();
            this.flags = new HashSet<>();
            this.positional = new ArrayList<>();
        }

        private Parsed(Map<String, String> values, Set<String> flags, List<String> positional) {
            this.values = Map.copyOf(values);
            this.flags = Set.copyOf(flags);
            this.positional = List.copyOf(positional);
        }

        public String getValue(String argument) {
            return values.get(argument);
        }

        public int getPositionalArgumentCount() {
            return positional.size();
        }

        public String getPositionalArgument(int index) {
            return positional.get(index);
        }

        public boolean hasFlag(String flag) {
            return flags.contains(flag);
        }


        public ConfigSection toConfigSection() {

            ConfigSection out = new ConfigSection();
            for(ArgumentType t : typesByName.values()) {

                if(t.isFlag()) {

                    out.set(t.name, flags.contains(t.name));

                } else {

                    if(values.containsKey(t.name)) {
                        out.set(t.name, values.get(t.name));
                    } else if(t.defaultValue != null) {
                        out.set(t.name, t.defaultValue);
                    }
                }
            }
            return out;
        }

    }


    public static class ArgumentType {

        private final String name;
        private final String defaultValue;
        private final Character alias;
        private final boolean flag;

        private ArgumentType(String name, String defaultValue, Character alias, boolean flag) {
            this.name = name;
            this.defaultValue = flag ? null : defaultValue;
            this.alias = alias;
            this.flag = flag;
        }

        public String getName() {
            return name;
        }

        public boolean hasAlias() {
            return alias != null;
        }

        public boolean isFlag() {
            return flag;
        }
    }

}

