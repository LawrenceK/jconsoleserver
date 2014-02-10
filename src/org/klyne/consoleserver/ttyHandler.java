package org.klyne.consoleserver;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.serial.SerialAddress;
import org.apache.mina.transport.serial.SerialAddress.*;
import org.apache.mina.transport.serial.SerialConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.klyne.consoleserver.Message;
import org.klyne.consoleserver.codec.StringCodecFactory;;

public class ttyHandler implements IoHandler {
    Logger logger = LoggerFactory.getLogger(ttyHandler.class);
	String name;
	private int baud = 9600;
	DataBits datasize = DataBits.DATABITS_8;
	StopBits stopbits = StopBits.BITS_2;
	IoSession session = null;
	Message messageHandler = null;

	public int getBaud() {
		return baud;
	}
	public void setBaud(int baud) {
		this.baud = baud;
	}

	public ttyHandler( String name, Message messageHandler )
	{
		this.name = name;
		this.messageHandler = messageHandler;
	}
	public void open() throws InterruptedException
	{
		IoConnector connector = new SerialConnector();
	    connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(
	            new StringCodecFactory())
//	            new TextLineCodecFactory())
	    		);
		connector.setHandler(this);
		SerialAddress portAddress = new SerialAddress(this.name, 
											this.getBaud(), 
											this.datasize, 
											this.stopbits, 
											Parity.NONE, 
											FlowControl.NONE);
		logger.info( "Connect " + portAddress );
		ConnectFuture future = connector.connect( portAddress );
		future.awaitUninterruptibly();
		this.session = future.getSession();
	}
	public void close()
	{
		if ( this.session != null ) {
			this.session.close();
		}
	}
	@Override
	public void exceptionCaught(IoSession arg0, Throwable arg1)
			throws Exception {
		// TODO Auto-generated method stub
		arg1.printStackTrace();
	}

	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		logger.info( "Inbound message " + message );
//		IoBuffer in = (IoBuffer)message;
		if ( this.messageHandler != null )
		{
			this.messageHandler.send(message.toString());
		}
		
	}
	@Override
	public void messageSent(IoSession arg0, Object arg1) throws Exception {
		logger.info("Sent " + arg1 );
		
	}
	@Override
	public void sessionClosed(IoSession arg0) throws Exception {
		logger.info("Closed " + this.name );
	}
	@Override
	public void sessionCreated(IoSession arg0) throws Exception {
		// TODO Auto-generated method stub
		logger.info("Created " + this.name );
	}
	@Override
	public void sessionIdle(IoSession arg0, IdleStatus arg1) throws Exception {
		logger.info("Idle " + this.name );
	}
	@Override
	public void sessionOpened(IoSession arg0) throws Exception {
		logger.info("Opened " + this.name );
	}

	public void write( String message) throws CharacterCodingException
	{
		WriteFuture wf = this.session.write(message);
//		wf.awaitUninterruptibly();
	}
}
