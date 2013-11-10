package org.ow2.chameleon.wisdom.content.engines;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.ow2.chameleon.wisdom.api.content.BodyParser;
import org.ow2.chameleon.wisdom.api.content.ContentEngine;
import org.ow2.chameleon.wisdom.api.content.ContentSerializer;
import org.ow2.chameleon.wisdom.api.http.Context;
import org.ow2.chameleon.wisdom.api.http.MimeTypes;
import org.ow2.chameleon.wisdom.api.http.Result;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.List;

/**
 * Content Engine.
 */
@Component
@Provides
@Instantiate(name = "ContentEngine")
public class Engine implements ContentEngine {

    @Requires(specification = BodyParser.class, optional = true)
    List<BodyParser> parsers;
    @Requires(specification = ContentSerializer.class, optional = true)
    List<ContentSerializer> serializers;

    @Override
    public BodyParser getBodyParserEngineForContentType(String contentType) {
        for (BodyParser parser : parsers) {
            if (parser.getContentType().equals(contentType)) {
                return parser;
            }
        }
        LoggerFactory.getLogger(this.getClass()).info("Cannot find a body parser for " + contentType);
        return null;
    }

    @Override
    public ContentSerializer getContentSerializerForContentType(String contentType) {
        for (ContentSerializer renderer : serializers) {
            if (renderer.getContentType().equals(contentType)) {
                return renderer;
            }
        }
        LoggerFactory.getLogger(this.getClass()).info("Cannot find a content renderer handling " + contentType);
        return null;
    }
}
