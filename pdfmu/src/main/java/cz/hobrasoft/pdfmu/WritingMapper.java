package cz.hobrasoft.pdfmu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An {@link ObjectMapper} that writes JSON documents to an {@link OutputStream}
 *
 * <p>
 * This class is a simple wrapper for {@link ObjectMapper} and
 * {@link OutputStream}.
 *
 * @author <a href="mailto:filip.bartek@hobrasoft.cz">Filip Bartek</a>
 */
public class WritingMapper {

    private final ObjectMapper mapper;
    private final OutputStream os;

    /**
     * Creates a {@link WritingMapper}
     *
     * @param mapper the {@link ObjectMapper} to use for writing JSON documents
     * @param os the target {@link OutputStream}
     */
    public WritingMapper(ObjectMapper mapper, OutputStream os) {
        this.mapper = mapper;
        this.os = os;
    }

    public WritingMapper() {
        this.mapper = new ObjectMapper(); // Create a new mapper
        mapper.enable(SerializationFeature.INDENT_OUTPUT); // Enable nice formatting
        this.os = System.err; // Bind to `System.err`
    }

    /**
     * Serializes a Java value as a JSON output, streaming it to the specified
     * {@link OutputStream}
     *
     * @param value the Java value to be serialized
     * @throws IOException if the underlying
     * {@link ObjectMapper#writeValue(OutputStream, Object)} throws an
     * {@link IOException}
     */
    public void writeValue(Object value) throws IOException {
        mapper.writeValue(os, value);
    }
}
