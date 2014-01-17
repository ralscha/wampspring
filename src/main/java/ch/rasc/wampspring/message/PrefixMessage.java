package ch.rasc.wampspring.message;

import java.io.IOException;
import java.io.StringWriter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * A client requests access to a valid topicURI to receive events published to
 * the given topicURI.
 * 
 * <p>
 * Auxiliary Messages
 * 
 * @see <a href="http://wamp.ws/spec/#prefix_message">WAMP specification</a>
 */
public class PrefixMessage extends WampMessage {

    private final String prefix;

    private final String uri;
    
    /**
     * @param type
     * @param prefix
     * @param uri
     */
    public PrefixMessage(String prefix, String uri) {
        super(WampMessageType.PREFIX);
        this.prefix = prefix;
        this.uri = uri;
    }
    
    public PrefixMessage(JsonParser jp) throws IOException {
        super(WampMessageType.PREFIX);

        if (jp.nextToken() != JsonToken.VALUE_STRING) {
            throw new IOException();
        }
        this.prefix = jp.getValueAsString();

        if (jp.nextToken() != JsonToken.VALUE_STRING) {
            throw new IOException();
        }
        this.uri = jp.getValueAsString();
    }
    
    /**
     * @return the prefix
     */
    public String getPrefix() {
        return prefix;
    }
    
    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    @Override
    public String toJson(JsonFactory jsonFactory) throws IOException {
        try (StringWriter sw = new StringWriter(); JsonGenerator jg = jsonFactory.createGenerator(sw)) {
            jg.writeStartArray();
            jg.writeNumber(getTypeId());
            jg.writeString(prefix);
            jg.writeString(uri);
            jg.writeEndArray();
            jg.close();
            return sw.toString();
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PrefixMessage [");
        if (prefix != null) {
            builder.append("prefix=");
            builder.append(prefix);
            builder.append(", ");
        }
        if (uri != null) {
            builder.append("uri=");
            builder.append(uri);
        }
        builder.append("]");
        return builder.toString();
    }


}