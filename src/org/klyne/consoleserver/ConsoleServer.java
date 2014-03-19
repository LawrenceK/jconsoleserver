package org.klyne.consoleserver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.lang.StringUtils;
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
		} catch (ConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

    Logger logger = LoggerFactory.getLogger(ConsoleServer.class);
	private int sshport = 8000;
	private SshServer sshd = null;
	private HashMap<String,portConfig> portConfigs = new HashMap<String,portConfig>();
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

	private void loadConfig() throws ConfigurationException
	{
		portConfig config;
		HierarchicalINIConfiguration ini = new HierarchicalINIConfiguration("./consoleserver.ini");
		ini.load();
		for ( String sectionName: ini.getSections() )
		{
			SubnodeConfiguration section = ini.getSection(sectionName);
			if (section.containsKey("baudrate"))
			{
				config = new portConfig(sectionName);
				config.setEnabled( section.getBoolean("enabled", true));
				config.setPort( section.getInt("sshport", 0));
				config.setBaud( section.getInt("baudrate", 9600));
				config.setParity(section.getString("parity", "N"));
				config.setDatasize(section.getInt("bytesize", 8));
				config.setStopbits(section.getString("stopbits", "2"));
				config.setTimeout(section.getInt("timeout", 0));
				config.setFlowXONXOFF( section.getBoolean("xonxoff", false));
				config.setFlowRTS( section.getBoolean("rtscts", false));
				portConfigs.put(config.getName(), config);
			}
		}
/*		
		for ( int index = 0; index < 32 ; index++)
		{
			config = new portConfig("/dev/ttyUSB" + index, this.sshport + 1 + index);
			portConfigs.put(config.getName(), config);
		}
*/
	}

	private void saveConfig() throws ConfigurationException
	{
		HierarchicalINIConfiguration ini = new HierarchicalINIConfiguration("./consoleserver.ini");
		ini.load();
		for (portConfig config : portConfigs.values()) 
		{
			SubnodeConfiguration section = ini.getSection(config.getName());
			section.setProperty("sshport", config.getPort());
			section.setProperty("enabled", config.isEnabled());
			section.setProperty("baudrate", config.getBaud());
			section.setProperty("parity", config.getParityStr());
			section.setProperty("bytesize", config.getDatasizeInt());
			section.setProperty("stopbits", config.getStopbitsStr());
			section.setProperty("timeout", config.getTimeout());
			section.setProperty("xonxoff", config.getFlowXONXOFF());
			section.setProperty("rtscts", config.getFlowRTS());
		}
		ini.save();
	}

	public void start() throws IOException, ConfigurationException
	/**
	 * Look for all USB connected terminal ports and create an SSH listener for the port.
	 * Start the master SSH listener. 
	 */
	{
		loadConfig();
		for (portConfig config : portConfigs.values()) 
		{
			File f = new File(config.getName()); 
			if ( f.exists() )
			{
				sshServers.put(config.getName(), new sshServer(config) );
			}
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

	class UnimplementedCommand extends BadCommand
	{
		String message;
		UnimplementedCommand( String message )
		{
			super(message);
		}
	}

	/**
	 * 
	 * @param name	terminal port name.
	 * @return The sshServer for the port.
	 * @throws BadCommand	if the port does not exist.
	 */
	portConfig get_port_config( String name ) throws BadCommand
	{
		portConfig config = portConfigs.get(name);
		if ( config == null )
		{
			throw new BadCommand("No such port");
		}
		return config;
	}

	public List<String> do_help(Scanner scanner) {
		ArrayList<String> result = new ArrayList<String>();
/*
 * TODO use reflection getMethods
		for (cliCommand cmd: cliCommands.values())
		{
			result.add(cmd.help);
		}
*/
		return result;
		
	}
	public List<String> do_list(Scanner scanner) {
		ArrayList<String> result = new ArrayList<String>();
		for (String key : portConfigs.keySet()) {
		    result.add(key);
		}
		return result;
	}
	public List<String> do_status(Scanner scanner) throws BadCommand {
		ArrayList<String> result = new ArrayList<String>();
		for (String key : sshServers.keySet()) {
		    result.add(key);
		}
		return result;
	}
	public List<String> do_show(Scanner scanner) throws BadCommand {
		portConfig config = get_port_config(scanner.next());
		return config.toStrings();
	}
	public List<String> do_exit(Scanner scanner) throws BadCommand {
		ArrayList<String> result = new ArrayList<String>();
		throw new UnimplementedCommand(this.help);
	}
	public List<String> do_commit(Scanner scanner) throws BadCommand {
		ArrayList<String> result = new ArrayList<String>();
		
		try {
			saveConfig();
			result.add("Ok - Saved");
		} catch (ConfigurationException e) {
			e.printStackTrace();
			result.add("Failed - Cannot write configuration file.");
		}
		return result;
	}
	public List<String> do_create(Scanner scanner) throws BadCommand {
		ArrayList<String> result = new ArrayList<String>();
		String newName = scanner.next();
		portConfig config = portConfigs.get(newName);
		if ( config == null )
		{
			config = new portConfig(newName);
			portConfigs.put(config.getName(), config);
			result.addAll( do_list(scanner) );
		}
		else
		{
			result.add( "Port allready exists" );
		}
		return result;
	}
	public List<String> do_stop(Scanner scanner) throws BadCommand {
		ArrayList<String> result = new ArrayList<String>();
		String name = scanner.next();
		// if name in sshServers then we can close it down.
		throw new UnimplementedCommand(this.help);
	}
	public List<String> do_start(Scanner scanner) throws BadCommand {
		ArrayList<String> result = new ArrayList<String>();
		String name = scanner.next();
		// if name not in sshServers and in portonfigs we can start it.
		throw new UnimplementedCommand(this.help);
	}
	public List<String> do_enable(Scanner scanner) throws BadCommand {
		portConfig config = get_port_config(scanner.next());
		config.setEnabled(scanner.nextBoolean());
		return config.toStrings();
	}
	public List<String> do_baud(Scanner scanner) throws BadCommand {
		portConfig config = get_port_config(scanner.next());
		config.setBaud(scanner.nextInt());;
		return config.toStrings();
	}
	public List<String> do_bytesize(Scanner scanner) throws BadCommand {
		portConfig config = get_port_config(scanner.next());
		config.setDatasize(scanner.nextInt());;
		return config.toStrings();
	}
	public List<String> do_stopbits(Scanner scanner) throws BadCommand {
		portConfig config = get_port_config(scanner.next());
		config.setStopbits(scanner.next());
		return config.toStrings();
	}
	public List<String> do_parity(Scanner scanner) throws BadCommand {
		portConfig config = get_port_config(scanner.next());
		config.setParity(scanner.next());
		return config.toStrings();
	}
	public List<String> do_rtscts(Scanner scanner) throws BadCommand {
		portConfig config = get_port_config(scanner.next());
		config.setFlowRTS(scanner.nextBoolean());
		return config.toStrings();
	}
	public List<String> do_xonxoff(Scanner scanner) throws BadCommand {
		portConfig config = get_port_config(scanner.next());
		config.setFlowXONXOFF(scanner.nextBoolean());
		return config.toStrings();
	}
	public List<String> do_sshport(Scanner scanner) throws BadCommand {
		portConfig config = get_port_config(scanner.next());
		config.setPort(scanner.nextInt());;
		return config.toStrings();
	}

	private class sessionHandler implements Command, ChannelDataReceiver, ChannelSessionAware
	{
		ChannelSession session;
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
			out.write(buf, start, len);	// echo
			commandBuffer.append(new String(buf, start, len, "UTF-8"));
			logger.info("ssh -> buffer " + commandBuffer);
			// Does it have and end of line?
			int eol = commandBuffer.indexOf("\r");
			if (eol >= 0)
			{
				// parse and process
				Scanner scanner = new Scanner(commandBuffer.substring(0,eol));
				commandBuffer.delete(0, eol+2);
				if (eol > 0)
				{
					String command = scanner.next();
					logger.info("Command -> " + command);
			        // method Scanner
					try {
				        Method commandMethod = this.getClass().getMethod("do"+command, new Class[]{Scanner.class});
				        Object result = commandMethod.invoke(this, scanner);
				        String response = StringUtils.join((String[])result, "\r\n");
						logger.info("Response -> " + response);
						out.write(response.getBytes("UTF8"));
					}
					catch (NoSuchMethodException e) {
						err.write(("Unrecognised command"+command).getBytes("UTF8"));
						e.printStackTrace();
					} 
					catch (SecurityException e) {
						err.write(("Unrecognised command or badly defined"+command).getBytes("UTF8"));
						e.printStackTrace();
					}
					catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
					catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
					catch (InvocationTargetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						if (e.getCause() instanceof UnimplementedCommand) 
						{
							err.write(("Unimplemented command : "+e).getBytes("UTF8"));
							e.printStackTrace();
						}	 
						else if (e.getCause() instanceof BadCommand) 
						{
							err.write(("Bad command : "+e).getBytes("UTF8"));
							e.printStackTrace();
						}
					}
				}
				err.flush();
				out.write( "\r\nOk> ".getBytes("UTF8") );
			}
			out.flush();
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
			this.session = session;
			this.session.setDataReceiver(this);
		}
	}
	
	@Override
	public Command create() {
		logger.info("create");
		return new sessionHandler();
	}

}
