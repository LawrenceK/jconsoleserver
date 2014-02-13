package org.klyne.consoleserver;

import org.apache.mina.transport.serial.SerialAddress.DataBits;
import org.apache.mina.transport.serial.SerialAddress.StopBits;

public class portConfig {
	// configuration for a single port, this is the terminal and the ssh server
	String name;
	int baud = 9600;
	DataBits datasize = DataBits.DATABITS_8;
	StopBits stopbits = StopBits.BITS_2;
	int port = 8000;

	public int getBaud() {
		return baud;
	}
	public void setBaud(int baud) {
		this.baud = baud;
	}
	public DataBits getDatasize() {
		return datasize;
	}
	public void setDatasize(DataBits datasize) {
		this.datasize = datasize;
	}
	public StopBits getStopbits() {
		return stopbits;
	}
	public void setStopbits(StopBits stopbits) {
		this.stopbits = stopbits;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public String getName() {
		return name;
	}
	public portConfig(String name, int port)
	{
		this.name = name;
		this.port = port;
	}
	public portConfig(String name, int port, int baud, StopBits stopbits, DataBits datasize)
	{
		this.name = name;
		this.port = port;
		this.baud = baud;
		this.datasize = datasize;
		this.stopbits = stopbits;
	}

}
