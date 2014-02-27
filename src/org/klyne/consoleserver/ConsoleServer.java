package org.klyne.consoleserver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

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
	/**
	 * ConsoleServer is the main entry in to the console server application. This class
	 * implements the command interface that can be used to reconfigure the terminal ports.
	 * 
	 * It can be connected to using SSH on port 8000.
	 * It creates further ssh servers see @sshServer that are responsible for providing the
	 * ssh access to a single terminal port. These are created on subsequent ports.
	 * 
	 */
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
		/**
		 * This creates the master SSH listener, the command interface.
		 */
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
	/**
	 * Look for all USB connected terminal ports and create an SSH listener for the port.
	 * Start the master SSH listener. 
	 */
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

	class BadCommand extends Exception
	{
		String message;
		BadCommand( String message )
		{
			this.message = message;
		}
		@Override
		public String toString() {
			return message;
		}
	}

	/**
	 * 
	 * @param name	terminal port name.
	 * @return The sshServer for the port.
	 * @throws BadCommand	if the port does not exist.
	 */
	sshServer get_port( String name ) throws BadCommand
	{
		sshServer server = sshServers.get(name);
		if ( server == null )
		{
			throw new BadCommand("No such port");
		}
		return server;
	}

	/**
	 * This is the basis of the command interface. We implement a new class for each possible command.
	 * 
	 * @author L.P.Klyne
	 *
	 */
	interface cliCommand{
		/**
		 * 
		 * @param scanner
		 * @return
		 */
		String execute(Scanner scanner) throws BadCommand;
	}

	static String cs_help = 
	           "help\n" +
	           "list [list configuration entries]\n" +
	           "status [list open ports]\n" +
	           "exit\n" +
	           "create <portname>\n" +
	           "show <portname>\n" +
	           "stop <portname>\n" +
	           "start <portname>\n" +
	           "enable <portname> <0,1>\n" +
	           "baud <portname> <baud>\n" +
	           "bytesize <portname> <5,6,7,8>\n" +
	           "stopbits <portname> <1,2>\n" +
	           "parity <portname> <N,E,O,M,S>\n" +
	           "rtscts <portname> <0,1>\n" +
	           "xonxoff <portname> <0,1>\n" +
	           "sshport <portname> <nnnn>\n";

	class do_help implements cliCommand
	{
		@Override
		public String execute(Scanner scanner) {
			return cs_help;
		}
	}
	class do_list implements cliCommand
	{
		@Override
		public String execute(Scanner scanner) {
			StringBuffer result = new StringBuffer(500);
			for (String key : sshServers.keySet()) {
			    result.append(key);
			    result.append("\n");
			}
			return result.toString();
		}
	}
	class do_status implements cliCommand
	{
		@Override
		public String execute(Scanner scanner) throws BadCommand {
			// is it connected
			portConfig config = get_port(scanner.next()).config();
			
			return String.format("Name : %s\n Baud : %i\n %i Bits\n %i stopbits\n",
						config.getName(), config.getBaud(), config.getDatasize(), config.getStopbits() );
		}
	}
	class do_show implements cliCommand
	{
		@Override
		public String execute(Scanner scanner) {
			return "Not Implemented";
		}
	}
	class do_exit implements cliCommand
	{
		@Override
		public String execute(Scanner scanner) {
			return "Not Implemented";
		}
	}
	class do_create implements cliCommand
	{
		@Override
		public String execute(Scanner scanner) {
			return "Not Implemented";
		}
	}
	class do_stop implements cliCommand
	{
		@Override
		public String execute(Scanner scanner) {
			return "Not Implemented";
		}
	}
	class do_start implements cliCommand
	{
		@Override
		public String execute(Scanner scanner) {
			return "Not Implemented";
		}
	}
	class do_enable implements cliCommand
	{
		@Override
		public String execute(Scanner scanner) {
			return "Not Implemented";
		}
	}
	class do_baud implements cliCommand
	{
		@Override
		public String execute(Scanner scanner) {
			return "Not Implemented";
		}
	}
	class do_bytesize implements cliCommand
	{
		@Override
		public String execute(Scanner scanner) {
			return "Not Implemented";
		}
	}
	class do_stopbits implements cliCommand
	{
		@Override
		public String execute(Scanner scanner) {
			return "Not Implemented";
		}
	}
	class do_parity implements cliCommand
	{
		@Override
		public String execute(Scanner scanner) {
			return "Not Implemented";
		}
	}
	class do_rtscts implements cliCommand
	{
		@Override
		public String execute(Scanner scanner) {
			return "Not Implemented";
		}
	}
	class do_xonxoff implements cliCommand
	{
		@Override
		public String execute(Scanner scanner) {
			return "Not Implemented";
		}
	}
	class do_sshport implements cliCommand
	{
		@Override
		public String execute(Scanner scanner) {
			return "Not Implemented";
		}
	}

	private HashMap<String, cliCommand> cliCommands = new HashMap<String, cliCommand>();
	{
	cliCommands.put("help", new do_help());
	cliCommands.put("list", new do_list());
	cliCommands.put("status", new do_status());
	cliCommands.put("exit", new do_exit());
	cliCommands.put("create", new do_create());
	cliCommands.put("show", new do_show());
	cliCommands.put("stop", new do_stop());
	cliCommands.put("start", new do_start());
	cliCommands.put("enable", new do_enable());
	cliCommands.put("baud", new do_baud());
	cliCommands.put("bytesize", new do_bytesize());
	cliCommands.put("stopbits", new do_stopbits());
	cliCommands.put("parity", new do_parity());
	cliCommands.put("rtscts", new do_rtscts());
	cliCommands.put("xonxoff", new do_xonxoff());
	cliCommands.put("sshport", new do_sshport());
	}

	private class sessionHandler implements Command, ChannelDataReceiver, ChannelSessionAware
	{
	    Logger logger = LoggerFactory.getLogger(sessionHandler.class);
	    StringBuffer commandBuffer = new StringBuffer(256);
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
			commandBuffer.append(new String(buf, start, len, "UTF-8"));
			logger.info("ssh -> buffer " + commandBuffer);
			// Does it have and end of line?
			int eol = commandBuffer.indexOf("\r\n");
			if (eol >= 0)
			{
				// parse and process
				Scanner scanner = new Scanner(commandBuffer.substring(0,eol));
				commandBuffer.delete(0, eol+2);
				String command = scanner.next();
				logger.info("Command -> " + command);
				if ( cliCommands.containsKey(command)) 
				{
					try {
						out.write(cliCommands.get(command).execute(scanner).getBytes("UTF8"));
					} catch (BadCommand e) {
						err.write(("Bad command : "+e).getBytes("UTF8"));
						e.printStackTrace();
					}
				}
				else 
				{
					err.write(("Unrecognised command"+command).getBytes("UTF8"));
				}
			}
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
