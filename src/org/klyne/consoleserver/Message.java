package org.klyne.consoleserver;

import org.apache.mina.core.buffer.IoBuffer;

public interface Message {
	void send(IoBuffer io);
}
