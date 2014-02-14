package org.klyne.consoleserver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleServer implements Factory<Command> {

	public static void main(String[] args) {
	
		ConsoleServer cServer = new ConsoleServer();
		try {
			cServer.start();
			while ( true )
			{
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

    Logger logger = LoggerFactory.getLogger(ConsoleServer.class);
	private int sshport = 8000;
	private SshServer sshd = null;
	private HashMap<String,sshServer> sshServers = new HashMap<String,sshServer>();
	
	public ConsoleServer()
	{
		
	}

	public void create_sshd() throws IOException
	{
		this.sshd = SshServer.setUpDefaultServer();
		this.sshd.setPort(this.sshport);
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

	public void start() throws IOException
	{
		portConfig config;
		for ( int index = 0; ; index++)
		{
			config = new portConfig("/dev/ttyUSB" + index, this.sshport + 1 + index);
			File f = new File(config.getName()); 
			if ( !f.exists() )
			{
				break;
			}
			sshServers.put(config.getName(), new sshServer(config) );
		}
		create_sshd();
	}

	private class sessionHandler implements Command, ChannelDataReceiver, ChannelSessionAware
	{
	    Logger logger = LoggerFactory.getLogger(sessionHandler.class);
		OutputStream out;
		OutputStream err;
		
		@Override
		public void destroy() {
			logger.info("destroy");
		}

		@Override
		public void setErrorStream(OutputStream arg0) {
			logger.info("setErrorStream");
			this.err = arg0;
		}

		@Override
		public void setExitCallback(ExitCallback arg0) {
			// TODO Auto-generated method stub
			logger.info("setExitCallback");
		}
		@Override
		public void setInputStream(InputStream arg0) {
			// Should not be called as we are ChannelSessionAware and register self as callback.
			logger.info("setInputStream");
			throw new IllegalArgumentException("Should not be called as we set a callback");
		}
		@Override
		public void setOutputStream(OutputStream arg0) {
			logger.info("setOutputStream");
			this.out = arg0;
		}

		@Override
		public void start(Environment arg0) throws IOException {
			logger.info("start");
		}

		@Override
		public int data(ChannelSession channel, byte[] buf, int start, int len)
				throws IOException {
			String command = new String(buf, start, len, "UTF-8");
			logger.info("ssh -> command " + command);
			return len;
		}

		@Override
		public void close() throws IOException {
			logger.info("close");
		}

		@Override
		public void setChannelSession(ChannelSession session) {
			// register for callback on data received.
			logger.info("setChannelSession");
			session.setDataReceiver(this);
		}
	}
	
	@Override
	public Command create() {
		logger.info("create");
		return new sessionHandler();
	}

}
