package org.klyne.consoleserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.Factory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelDataReceiver;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;

public class sshServer implements Factory<Command> {
	private SshServer sshd = null;
	private ttyHandler tty = null;
	
	private class sessionHandler implements Command, ChannelDataReceiver
	{
		@Override
		public void destroy() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setErrorStream(OutputStream arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setExitCallback(ExitCallback arg0) {
			// TODO Auto-generated method stub
			
		}
		@Override
		public void setInputStream(InputStream arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setOutputStream(OutputStream arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void start(Environment arg0) throws IOException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public int data(ChannelSession channel, byte[] buf, int start, int len)
				throws IOException {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void close() throws IOException {
			// TODO Auto-generated method stub
			
		}
	}
	
	private class commandHandler extends sessionHandler
	{
		// The root port server runs a command shell.
	}

	@Override
	public Command create() {
		// TODO Auto-generated method stub
		return new sessionHandler();
	}

	public sshServer(ttyHandler tty) throws IOException
	{
		this.tty = tty;
		this.sshd = SshServer.setUpDefaultServer();
		this.sshd.setPort(8000);
		this.sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("hostkey.ser"));
		this.sshd.setShellFactory( this );
		this.sshd.start();
	}
}
