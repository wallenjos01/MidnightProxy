package org.wallentines.mdproxy.util;

import org.wallentines.mdcfg.serializer.InlineSerializer;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.pseudonym.*;
import org.wallentines.pseudonym.text.Component;
import org.wallentines.pseudonym.text.ConfigTextParser;
import org.wallentines.pseudonym.text.Content;

public class MessageUtil {

    public static final PlaceholderManager PLACEHOLDERS = new PlaceholderManager();

    public static final MessagePipeline<String, UnresolvedMessage<String>> PARSE_PIPELINE = MessagePipeline.<String>builder()
            .add(new PlaceholderParser(PLACEHOLDERS))
            .build();

    public static final Serializer<UnresolvedMessage<String>> PARSE_SERIALIZER = InlineSerializer.of(
            un -> UnresolvedMessage.resolve(un, PipelineContext.EMPTY),
            PARSE_PIPELINE::accept
    );

    public static final Serializer<Component> CONFIG_SERIALIZER = InlineSerializer.of(ConfigTextParser.INSTANCE::serialize, ConfigTextParser.INSTANCE::parse);


    public static String flatten(Component component) {

        StringBuilder builder = new StringBuilder();
        if(component.content().type() == Content.Type.TRANSLATE) {
            builder.append(component.content());
        }
        for(Component c : component.children()) {
            builder.append(flatten(c));
        }

        return builder.toString();
    }

}
