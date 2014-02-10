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
		private StringBuffer messages = new StringBuffer(); 
		@Override
		public void send(String str) {
		    logger.info(str);
			messages.append(str);
		}
		@Override
		public String toString() {
			return messages.toString();
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
		this.handler = new ttyHandler("/dev/ttyUSB1", this.messages);
	}

	@After
	public void tearDown() throws Exception {
		if ( this.handler != null )
		{
			this.handler.close();
			this.handler = null;
		}
		this.messages = null;
	}

	@Test
	public void testGetBaud() {
		assertEquals(9600, this.handler.getBaud());
	}

	@Test
	public void testSetBaud() {
		assertEquals(9600, this.handler.getBaud());
		this.handler.setBaud(19200);
		assertEquals(19200, this.handler.getBaud());
	}

	@Test
	public void testOpenClose() {
		try {
			this.handler.open();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.handler.close();
		this.handler = null;
	}

	@Test
	public void testLoopback() {
		try {
			this.handler.open();
			this.handler.write( "Hello123" );
			Thread.sleep(2000);
			this.handler.close();
			this.handler = null;
			assertEquals("Hello123", messages.toString() );
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CharacterCodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
