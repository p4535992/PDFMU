package cz.hobrasoft.pdfmu;

import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import org.apache.commons.io.output.NullOutputStream;
import org.json.JSONException;
import org.json.JSONWriter;

/**
 * An extension of {@link JSONWriter}
 *
 * <p>
 * Exposes writer flushing in {@link #flush()}. Adds shortcuts for common
 * combinations of operations.
 *
 * <p>
 * Note that the methods do not return the {@link JSONWriter} instance, so
 * cascade style chains of operations are not possible using
 * {@link JSONWriterEx}.
 *
 * <p>
 * Note that an instance of {@link JSONWriter} (and by extension of
 * {@link JSONWriterEx}) maintains an internal JSON document context and can
 * only be used for writing a single JSON document. This holds for discarding
 * writers as well (namely writers instantiated by calling
 * {@link #JSONWriterEx()}) - in all cases a new instance must be created for
 * every JSON document to be written or discarded.
 *
 * @author <a href="mailto:filip.bartek@hobrasoft.cz">Filip Bartek</a>
 */
public class JSONWriterEx extends JSONWriter implements Flushable {

    /**
     * Makes a JSON writer that outputs data to a {@link Writer}.
     *
     * @param w the writer to write the JSON-formatted data to
     */
    public JSONWriterEx(Writer w) {
        super(w);
    }

    /**
     * Makes a JSON writer that outputs data to an {@link OutputStream}.
     *
     * <p>
     * Common uses:
     * <ul>
     * <li>{@code JSONWriterEx(System.out)} - standard output stream
     * <li>{@code JSONWriterEx(System.err)} - standard error output stream
     * <li>{@code JSONWriterEx(NullOutputStream.NULL_OUTPUT_STREAM)} - null
     * (that is discarding) output stream; implemented in
     * {@link #JSONWriterEx()}
     * </ul>
     *
     * @param os the output stream to writer the JSON-formatted data to
     */
    public JSONWriterEx(OutputStream os) {
        this(new OutputStreamWriter(os));
    }

    /**
     * Makes a JSON writer that discards all the received data.
     *
     * <p>
     * All the data is sent to {@link NullOutputStream#NULL_OUTPUT_STREAM}.
     */
    public JSONWriterEx() {
        this(NullOutputStream.NULL_OUTPUT_STREAM);
    }

    /**
     * Flushes the underlying writer by writing any buffered output to the
     * underlying stream. Makes the data recorded so far available to the
     * listener of the stream.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    /**
     * Appends a key-value pair.
     *
     * <p>
     * Appends a key and its associated atomic value.
     *
     * <p>
     * Wraps {@link JSONWriter#key(String)} and one of the following:
     * <ul>
     * <li>{@link JSONWriter#value(boolean)}
     * <li>{@link JSONWriter#value(double)}
     * <li>{@link JSONWriter#value(long)}
     * <li>{@link JSONWriter#value(Object)}
     * </ul>
     *
     * <p>
     * Inspiration:
     * {@link javax.json.stream.JsonGenerator#write(String, String)}
     *
     * @param <V> value type (supported types: boolean, double, long,
     * {@link Object})
     * @param key key string
     * @param value value
     */
    public <V> void write(String key, V value) {
        this.key(key);
        // `this.value` accepts `Object`, so the following call will always succeed.
        // `this.value` also accepts (more) primitive types, so genericity is useful.
        this.value(value);
    }

    /**
     * Begins appending a new array associated with a given key.
     *
     * <p>
     * Appends a key and begins appending a new array as the value associated
     * with the key.
     *
     * <p>
     * Wraps {@link JSONWriter#key(String)} and {@link JSONWriter#array()}.
     *
     * <p>
     * Inspiration:
     * {@link javax.json.stream.JsonGenerator#writeStartArray(String)}
     *
     * @param key key string
     */
    public void array(String key) {
        this.key(key);
        array();
    }

    /**
     * Begin appending a new object associated with a given key.
     *
     * <p>
     * Appends a key and begins appending a new object as the value associated
     * with the key.
     *
     * <p>
     * Wraps {@link JSONWriter#key(String)} and {@link JSONWriter#object()}.
     *
     * <p>
     * Inspiration:
     * {@link javax.json.stream.JsonGenerator#writeStartObject(String)}
     *
     * @param key key string
     */
    public void object(String key) {
        this.key(key);
        object();
    }

    /**
     * Ends the current array or object.
     *
     * <p>
     * This method must be called to balance a call to
     * {@link JSONWriter#array()} or {@link JSONWriter#object()}. Calls either
     * {@link JSONWriter#endArray()} or {@link JSONWriter#endObject()} based on
     * context.
     *
     * @throws JSONException if the current context of the JSON document does
     * not allow ending and array or an object
     */
    public void end() throws JSONException {
        switch (mode) {
            case 'k':
                // Expecting key in an object
                endObject();
                break;
            case 'a':
                // Expecting an array element
                endArray();
                break;
            default:
                throw new JSONException("Misplaced end.");
        }
    }

}