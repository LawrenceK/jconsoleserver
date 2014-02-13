package org.klyne.consoleserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.Factory;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.ChannelSessionAware;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.auth.UserAuthNone;
import org.apache.sshd.server.channel.ChannelDataReceiver;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.klyne.consoleserver.ttyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class sshServer implements Factory<Command> {
    Logger logger = LoggerFactory.getLogger(ttyHandler.class);
	// This manages an ssh server on a TCP port.
	// this may be connected directly to a serial port
	// or may be the master server that has a command interface on it.
	private SshServer sshd = null;
	private portConfig config = null;
	
	
	private class sessionHandler implements Command, ChannelDataReceiver, ChannelSessionAware, Message
	{
		OutputStream out;
		private ttyHandler tty = null;
		
		@Override
		public void destroy() {
			tty.messageHandler = null;
		}

		@Override
		public void setErrorStream(OutputStream arg0) {
			// a serial terminal only has a single stream of output so ignore the ssh
			// error channel
		}

		@Override
		public void setExitCallback(ExitCallback arg0) {
			// TODO Auto-generated method stub
		}
		@Override
		public void setInputStream(InputStream arg0) {
			// Should not be called as we are ChannelSessionAware and register self as callback.
			throw new IllegalArgumentException("Should not be called as we set a callback");
		}

		@Override
		public void setOutputStream(OutputStream arg0) {
			this.out = arg0;
		}

		@Override
		public void start(Environment arg0) throws IOException {
			// TODO Auto-generated method stub
			this.tty = new ttyHandler(config, this);
			this.tty.open();
		}

		@Override
		public int data(ChannelSession channel, byte[] buf, int start, int len)
				throws IOException {
			IoBuffer io = IoBuffer.wrap( buf, start, len ); 
			logger.info("ssh -> tty " + io);
			tty.write( io );
			return len;
		}

		@Override
		public void close() throws IOException {
			this.tty.close();
			this.tty = null;
		}

		@Override
		public void setChannelSession(ChannelSession session) {
			// register for callback on data received.
			session.setDataReceiver(this);
			
		}

		@Override
		public void send(IoBuffer io){
			// the ttyHandler will call this.
			try {
				logger.info("tty -> ssh " + io);
				out.write(io.array(), 0, io.remaining());
				out.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private class commandHandler extends sessionHandler
	{
		@Override
		public int data(ChannelSession channel, byte[] buf, int start, int len)
				throws IOException {
			// TODO add command buffering processing here.
			return super.data(channel, buf, start, len);
		}
	}

	@Override
	public Command create() {
		// TODO Auto-generated method stub
		return new sessionHandler();
	}

	public sshServer(portConfig config) throws IOException
	{
		this.config = config; 
		this.sshd = SshServer.setUpDefaultServer();
		this.sshd.setPort(config.getPort());
		this.sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("hostkey.ser"));
		
		List<NamedFactory<UserAuth>> userAuthFactories = new ArrayList<NamedFactory<UserAuth>>();
	    userAuthFactories.add(new UserAuthNone.Factory());
	    sshd.setUserAuthFactories(userAuthFactories);
	    
//	    sshd.setPasswordAuthenticator(new MyPasswordAuthenticator());
//	    sshd.setPublickeyAuthenticator(new MyPublickeyAuthenticator());
//		this.sshd.setPublickeyAuthenticator();
		
		this.sshd.setShellFactory( this );
		this.sshd.start();
	}
}
