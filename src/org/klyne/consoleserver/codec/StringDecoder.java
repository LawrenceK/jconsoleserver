package org.klyne.consoleserver.codec;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

import org.apache.mina.core.buffer.BufferDataException;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.RecoverableProtocolDecoderException;

/**
 * A {@link ProtocolDecoder} which decodes a text line into a string.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class StringDecoder implements ProtocolDecoder {
    private final AttributeKey DECODER = new AttributeKey(getClass(), "decoder");

    private final Charset charset;

    /** The default maximum Line length. Default to 1024. */
    private int maxLineLength = 1024;

    /** The default maximum buffer length. Default to 128 chars. */
    private int bufferLength = 128;

    /**
     * Creates a new instance with the current default {@link Charset}
     * and {@link LineDelimiter#AUTO} delimiter.
     */
    public StringDecoder() {
        this(Charset.defaultCharset());
    }

    /**
     * Creates a new instance with the specified <tt>charset</tt>
     * and the specified <tt>delimiter</tt>.
     */
    public StringDecoder(Charset charset) {
        if (charset == null) {
            throw new IllegalArgumentException("charset parameter shuld not be null");
        }

        this.charset = charset;
    }

    /**
     * Returns the allowed maximum size of the line to be decoded.
     * If the size of the line to be decoded exceeds this value, the
     * decoder will throw a {@link BufferDataException}.  The default
     * value is <tt>1024</tt> (1KB).
     */
    public int getMaxLineLength() {
        return maxLineLength;
    }

    /**
     * Sets the allowed maximum size of the line to be decoded.
     * If the size of the line to be decoded exceeds this value, the
     * decoder will throw a {@link BufferDataException}.  The default
     * value is <tt>1024</tt> (1KB).
     */
    public void setMaxLineLength(int maxLineLength) {
        if (maxLineLength <= 0) {
            throw new IllegalArgumentException("maxLineLength (" + maxLineLength + ") should be a positive value");
        }

        this.maxLineLength = maxLineLength;
    }

    /**
     * Sets the default buffer size. This buffer is used in the Context
     * to store the decoded line.
     *
     * @param bufferLength The default bufer size
     */
    public void setBufferLength(int bufferLength) {
        if (bufferLength <= 0) {
            throw new IllegalArgumentException("bufferLength (" + maxLineLength + ") should be a positive value");

        }

        this.bufferLength = bufferLength;
    }

    /**
     * Returns the allowed buffer size used to store the decoded line
     * in the Context instance.
     */
    public int getBufferLength() {
        return bufferLength;
    }

    /**
     * {@inheritDoc}
     */
    public void decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
        CharsetDecoder decoder = (CharsetDecoder) session.getAttribute(DECODER);

        if (decoder == null) {
        	decoder = charset.newDecoder();
            session.setAttribute(DECODER, decoder);
        }

        byte[] data = new byte[in.limit()];
        in.get(data);
        CharBuffer buffer = decoder.decode(ByteBuffer.wrap(data));
        String str = new String(buffer.array());
        writeText(session, str, out);
    }

    /**
     * {@inheritDoc}
     */
    public void finishDecode(IoSession session, ProtocolDecoderOutput out) throws Exception {
        // Do nothing
    }

    /**
     * {@inheritDoc}
     */
    public void dispose(IoSession session) throws Exception {
    }

    /**
     * By default, this method propagates the decoded line of text to
     * {@code ProtocolDecoderOutput#write(Object)}.  You may override this method to modify
     * the default behavior.
     *
     * @param session  the {@code IoSession} the received data.
     * @param text  the decoded text
     * @param out  the upstream {@code ProtocolDecoderOutput}.
     */
    protected void writeText(IoSession session, String text, ProtocolDecoderOutput out) {
        out.write(text);
    }
}