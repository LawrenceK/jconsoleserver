package org.klyne.consoleserver;

import static org.junit.Assert.*;

import org.apache.mina.transport.serial.SerialAddress.DataBits;
import org.apache.mina.transport.serial.SerialAddress.StopBits;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class testPortConfig {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGet() {
		portConfig config = new portConfig("name", 8000, 9600, StopBits.BITS_2, DataBits.DATABITS_8);
		assertEquals("name", config.getName());
		assertEquals(8000, config.getPort());
		assertEquals(9600, config.getBaud());
		assertEquals(StopBits.BITS_2, config.getStopbits());
		assertEquals(DataBits.DATABITS_8, config.getDatasize());
	}

	@Test
	public void testSet() {
		portConfig config = new portConfig("name", 8000, 9600, StopBits.BITS_2, DataBits.DATABITS_8);
		config.setBaud(19200);
		config.setPort(8001);
		config.setDatasize(7);
		config.setStopbits("1");
		assertEquals("name", config.getName());
		assertEquals(8001, config.getPort());
		assertEquals(19200, config.getBaud());
		assertEquals(StopBits.BITS_1, config.getStopbits());
		assertEquals(DataBits.DATABITS_7, config.getDatasize());
	}

}
