package org.klyne.consoleserver;

import java.nio.charset.CharacterCodingException;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.serial.SerialAddress;
import org.apache.mina.transport.serial.SerialAddress.DataBits;
import org.apache.mina.transport.serial.SerialAddress.FlowControl;
import org.apache.mina.transport.serial.SerialAddress.Parity;
import org.apache.mina.transport.serial.SerialAddress.StopBits;
import org.apache.mina.transport.serial.SerialConnector;
import org.klyne.consoleserver.codec.NullCodecFactory;
import org.klyne.consoleserver.codec.StringCodecFactory;
import org.klyne.consoleserver.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ttyHandler implements IoHandler {
    Logger logger = LoggerFactory.getLogger(ttyHandler.class);
    portConfig config;
	IoSession session = null;
	Message messageHandler = null;

	public boolean isClosed()
	{
		return this.session == null;
	}

	public ttyHandler( portConfig config, Message messageHandler )
	{
		this.config = config;
		this.messageHandler = messageHandler;
	}
	public void open()
	{
		IoConnector connector = new SerialConnector();
//	    connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(
//	            new NullCodecFactory())
//	            new StringCodecFactory())
//	            new TextLineCodecFactory())
//	    		);
		connector.setHandler(this);
		SerialAddress portAddress = new SerialAddress(this.config.name, 
											this.config.baud, 
											this.config.datasize, 
											this.config.stopbits, 
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
			this.session.close(false);
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
		IoBuffer io = (IoBuffer)message;
		if ( this.messageHandler != null )
		{
			this.messageHandler.send(io);
		}
		
	}
	@Override
	public void messageSent(IoSession arg0, Object arg1) throws Exception {
		logger.info("Sent " + arg1 );
		
	}
	@Override
	public void sessionClosed(IoSession arg0) throws Exception {
		logger.info("Closed " + this.config.name );
		this.session = null;
	}
	@Override
	public void sessionCreated(IoSession arg0) throws Exception {
		// TODO Auto-generated method stub
		logger.info("Created " + this.config.name );
	}
	@Override
	public void sessionIdle(IoSession arg0, IdleStatus arg1) throws Exception {
		logger.info("Idle " + this.config.name );
	}
	@Override
	public void sessionOpened(IoSession arg0) throws Exception {
		logger.info("Opened " + this.config.name );
	}

	public void write(Object message) throws CharacterCodingException
	{
		this.session.write(message);
	}
}
