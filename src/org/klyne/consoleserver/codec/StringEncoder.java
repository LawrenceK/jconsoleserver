package org.klyne.consoleserver.codec;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

/**
 * A {@link ProtocolEncoder} which encodes a string into a text line
 * which ends with the delimiter.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class StringEncoder extends ProtocolEncoderAdapter {
    private final AttributeKey ENCODER = new AttributeKey(getClass(), "encoder");

    private final Charset charset;

    private int maxLineLength = Integer.MAX_VALUE;

    /**
     * Creates a new instance with the current default {@link Charset}
     * and {@link LineDelimiter#UNIX} delimiter.
     */
    public StringEncoder() {
        this(Charset.defaultCharset());
    }

    /**
     * Creates a new instance with the spcified <tt>charset</tt>
     * and the specified <tt>delimiter</tt>.
     */
    public StringEncoder(Charset charset) {
        if (charset == null) {
            throw new IllegalArgumentException("charset");
        }

        this.charset = charset;
    }

    /**
     * Returns the allowed maximum size of the encoded line.
     * If the size of the encoded line exceeds this value, the encoder
     * will throw a {@link IllegalArgumentException}.  The default value
     * is {@link Integer#MAX_VALUE}.
     */
    public int getMaxLineLength() {
        return maxLineLength;
    }

    /**
     * Sets the allowed maximum size of the encoded line.
     * If the size of the encoded line exceeds this value, the encoder
     * will throw a {@link IllegalArgumentException}.  The default value
     * is {@link Integer#MAX_VALUE}.
     */
    public void setMaxLineLength(int maxLineLength) {
        if (maxLineLength <= 0) {
            throw new IllegalArgumentException("maxLineLength: " + maxLineLength);
        }

        this.maxLineLength = maxLineLength;
    }

    public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
        CharsetEncoder encoder = (CharsetEncoder) session.getAttribute(ENCODER);

        if (encoder == null) {
            encoder = charset.newEncoder();
            session.setAttribute(ENCODER, encoder);
        }

        String value = (message == null ? "" : message.toString());
        IoBuffer buf = IoBuffer.allocate(value.length()).setAutoExpand(true);
        buf.putString(value, encoder);

        if (buf.position() > maxLineLength) {
            throw new IllegalArgumentException("Line length: " + buf.position());
        }

        buf.flip();
        out.write(buf);
        out.flush();
    }

    public void dispose() throws Exception {
        // Do nothing
    }
}