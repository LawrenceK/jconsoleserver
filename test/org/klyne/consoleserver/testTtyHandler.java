package org.klyne.consoleserver;
import static org.junit.Assert.*;

import java.nio.charset.CharacterCodingException;
import java.sql.Time;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.mina.core.buffer.IoBuffer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.klyne.consoleserver.Message;
import org.klyne.consoleserver.ttyHandler;

public class testTtyHandler {
	ttyHandler handler = null;
    Logger logger = LoggerFactory.getLogger(testTtyHandler.class);
	
	class messageHandler implements Message {
		private IoBuffer messages = IoBuffer.allocate(1000);
		@Override
		public void send(IoBuffer io) {
			messages.put(io);
		}
		public IoBuffer value() {
			messages.flip();	// reset so readable.
			return messages;
		}
		
	}
	messageHandler messages = null;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		this.messages = new messageHandler();
		this.handler = new ttyHandler( new portConfig("/dev/ttyUSB1", 8000), this.messages);
	}

	@After
	public void tearDown() throws Exception {
		this.doClose();
		this.messages = null;
	}
	private void doClose()
	{
		if ( this.handler != null )
		{
			this.handler.close();
			while ( ! this.handler.isClosed() )
			{
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			this.handler = null;
		}
	}

	@Test
	public void testOpenClose() {
		this.handler.open();
		this.doClose();
	}

	@Test
	public void testLoopback() {
		try {
			this.handler.open();
			IoBuffer msg = IoBuffer.wrap( "Hello123".getBytes());
			this.handler.write( msg );
			Thread.sleep(2000);
			assertEquals( msg, messages.value() );
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CharacterCodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
