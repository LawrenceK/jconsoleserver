package org.klyne.consoleserver;

import java.util.ArrayList;
import java.util.List;

import org.apache.mina.transport.serial.SerialAddress.DataBits;
import org.apache.mina.transport.serial.SerialAddress.StopBits;
import org.apache.mina.transport.serial.SerialAddress.FlowControl;
import org.apache.mina.transport.serial.SerialAddress.Parity;

public class portConfig {
	// configuration for a single port, this is the terminal and the ssh server
	String name;
	boolean enabled = true;
	int baud = 9600;
	DataBits datasize = DataBits.DATABITS_8;
	StopBits stopbits = StopBits.BITS_2;
	int port = 8000;
	Parity parity = Parity.NONE;
	FlowControl flowControl = FlowControl.NONE;
	int timeout = 0;

	public boolean isEnabled() {
		return this.enabled;
	}
	public void setEnabled(boolean enable) {
		this.enabled = enable;
	}
	public int getBaud() {
		return baud;
	}
	public void setBaud(int baud) {
		this.baud = baud;
	}
	public DataBits getDatasize() {
		return datasize;
	}
	public int getDatasizeInt() {
		switch (this.datasize)
		{
		case DATABITS_8:
			return 8;
		case DATABITS_7:
			return 7;
		case DATABITS_6:
			return 6;
		case DATABITS_5:
			return 5;
		}
		return 8;
	}
	public void setDatasize(int datasize) {
		switch (datasize)
		{
		case 8:
			this.datasize = DataBits.DATABITS_8;
			break;
		case 7:
			this.datasize = DataBits.DATABITS_7;
			break;
		case 6:
			this.datasize = DataBits.DATABITS_6;
			break;
		case 5:
			this.datasize = DataBits.DATABITS_5;
			break;
		default:
			// log bad value
			this.datasize = DataBits.DATABITS_8;
		}
	}
	public StopBits getStopbits() {
		return stopbits;
	}
	public String getStopbitsStr() {
		switch (this.stopbits)
		{
		case BITS_2:
			return "2";
		case BITS_1:
			return "1";
		case BITS_1_5:
			return "1.5";
		default:
			break;
		}
		return "2";
	}
	public void setStopbits(String stopbits) {
		if (stopbits.length() > 0)
		{
			switch (stopbits.trim())
			{
			case "2":
				this.stopbits = StopBits.BITS_2;
				break;
			case "1":
				this.stopbits = StopBits.BITS_1;
				break;
			case "1.5":
				this.stopbits = StopBits.BITS_1_5;
				break;
			default:
				// log bad value
			}
		}
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public Parity getParity() {
		return this.parity;
	}
	public String getParityStr() {
		switch (this.parity)
		{
		case MARK:
			return "M";
		case SPACE:
			return "S";
		case EVEN:
			return "E";
		case ODD:
			return "O";
		case NONE:
			return "N";
		}
		return "N";
	}
	public void setParity(String parity) {
		if (parity.length() > 0)
		{
			switch (parity.charAt(0))
			{
			case 'N':
				this.parity = Parity.NONE;
				break;
			case 'E':
				this.parity = Parity.EVEN;
				break;
			case 'O':
				this.parity = Parity.ODD;
				break;
			case 'M':
				this.parity = Parity.MARK;
				break;
			case 'S':
				this.parity = Parity.SPACE;
				break;
			default:
				// log bad value
			}
		}
	}
	public FlowControl getFlowControl() {
		return this.flowControl;
	}
	public String getFlowRTS() {
		return "0";
	}
	public String getFlowXONXOFF() {
		return "0";
	}
	public void setFlowRTS(boolean RTS) {
	}
	public void setFlowXONXOFF(boolean doXON) {
	}
	public int getTimeout() {
		return timeout;
	}
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	public String getName() {
		return name;
	}
	public String getFileName() {
		// Place holder, allows the name and device file to be different.
		return name;
	}
	public portConfig(String name)
	{
		this.name = name;
	}
	public portConfig(String name, int port, int baud, StopBits stopbits, DataBits datasize)
	{
		this.name = name;
		this.port = port;
		this.baud = baud;
		this.datasize = datasize;
		this.stopbits = stopbits;
	}
	public List<String> toStrings()
	{
		ArrayList<String> result = new ArrayList<String>();
		result.add( "Name : " + this.name);
		result.add( "Device : " + getFileName() );
		result.add( "Enabled : " + isEnabled() );
		result.add( String.format("Settings : %i:%i:%s:%i",this.baud, getDatasizeInt(), getParityStr(), getStopbitsStr()));  
		result.add( String.format("FlowControl %s:%s",getFlowRTS(), getFlowXONXOFF())); 
		result.add( "SSHPort : " + getPort() );
		result.add( "Timeout : " + getTimeout() );
		return result;
	}
	public String toString()
	{
		return String.format("Name : %s\nDevice %s\nEnabled %s\nSettings : %i:%i:%s:%i\n FlowControl %s:%s\nSSHPort %i\nTimeout %i\n",
				this.name, getFileName(), isEnabled(), 
				this.baud, getDatasizeInt(), getParityStr(), getStopbitsStr(),
				getFlowRTS(), getFlowXONXOFF(), 
				getPort(), getTimeout() );
	}
}
