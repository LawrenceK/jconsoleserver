package org.klyne.consoleserver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
			section.setProperty("enable", config.isEnabled());
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

	/**
	 * This is the basis of the command interface. We implement a new class for each possible command.
	 * 
	 * @author L.P.Klyne
	 *
	 */
	abstract class cliCommand {
		String help; 
		/**
		 * 
		 * @param scanner
		 * @return
		 */
		abstract List<String> execute(Scanner scanner) throws BadCommand;
	}

	class do_help extends cliCommand
	{
		String help = "help";
		@Override
		public List<String> execute(Scanner scanner) {
			ArrayList<String> result = new ArrayList<String>();
			for (cliCommand cmd: cliCommands.values())
			{
				result.add(cmd.help);
			}
			return result;
		}
	}
	class do_list extends cliCommand
	{
		String help = "list [list configuration entries]";
		@Override
		public List<String> execute(Scanner scanner) {
			ArrayList<String> result = new ArrayList<String>();
			for (String key : portConfigs.keySet()) {
			    result.add(key);
			}
			return result;
		}
	}
	class do_status extends cliCommand
	{
		String help = "status [list open ports]";
		@Override
		public List<String> execute(Scanner scanner) throws BadCommand {
			ArrayList<String> result = new ArrayList<String>();
			for (String key : sshServers.keySet()) {
			    result.add(key);
			}
			return result;
		}
	}
	class do_show extends cliCommand
	{
		String help = "show <portname>";
		@Override
		public List<String> execute(Scanner scanner) throws BadCommand {
			portConfig config = get_port_config(scanner.next());
			return config.toStrings();
		}
	}
	class do_exit extends cliCommand
	{
		String help = "exit";
		@Override
		public List<String> execute(Scanner scanner) throws BadCommand {
			ArrayList<String> result = new ArrayList<String>();
			throw new UnimplementedCommand(this.help);
		}
	}
	class do_commit extends cliCommand
	{
		String help = "commit";
		@Override
		public List<String> execute(Scanner scanner) throws BadCommand {
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
	}
	class do_create extends cliCommand
	{
		String help = "create <portname>";
		@Override
		public List<String> execute(Scanner scanner) throws BadCommand {
			ArrayList<String> result = new ArrayList<String>();
			String newName = scanner.next();
			portConfig config = portConfigs.get(newName);
			if ( config == null )
			{
				config = new portConfig(newName);
				portConfigs.put(config.getName(), config);
				result.addAll( cliCommands.get("list").execute(scanner));
			}
			else
			{
				result.add( "Port allready exists" );
			}
			return result;
		}
	}
	class do_stop extends cliCommand
	{
		String help = "stop <portname>";
		@Override
		public List<String> execute(Scanner scanner) throws BadCommand {
			ArrayList<String> result = new ArrayList<String>();
			String name = scanner.next();
			// if name in sshServers then we can close it down.
			throw new UnimplementedCommand(this.help);
		}
	}
	class do_start extends cliCommand
	{
		String help = "start <portname>";
		@Override
		public List<String> execute(Scanner scanner) throws BadCommand {
			ArrayList<String> result = new ArrayList<String>();
			String name = scanner.next();
			// if name not in sshServers and in portonfigs we can start it.
			throw new UnimplementedCommand(this.help);
		}
	}
	class do_enable extends cliCommand
	{
		String help = "enable <portname> <yes,no>";
		@Override
		public List<String> execute(Scanner scanner) throws BadCommand {
			portConfig config = get_port_config(scanner.next());
			config.setEnabled(scanner.nextBoolean());
			return config.toStrings();
		}
	}
	class do_baud extends cliCommand
	{
		String help = "baud <portname> <baud>";
		@Override
		public List<String> execute(Scanner scanner) throws BadCommand {
			portConfig config = get_port_config(scanner.next());
			config.setBaud(scanner.nextInt());;
			return config.toStrings();
		}
	}
	class do_bytesize extends cliCommand
	{
		String help = "bytesize <portname> <5,6,7,8>";
		@Override
		public List<String> execute(Scanner scanner) throws BadCommand {
			portConfig config = get_port_config(scanner.next());
			config.setDatasize(scanner.nextInt());;
			return config.toStrings();
		}
	}
	class do_stopbits extends cliCommand
	{
		String help = "stopbits <portname> <1,2>";
		@Override
		public List<String> execute(Scanner scanner) throws BadCommand {
			portConfig config = get_port_config(scanner.next());
			config.setStopbits(scanner.next());
			return config.toStrings();
		}
	}
	class do_parity extends cliCommand
	{
		String help = "parity <portname> <N,E,O,M,S>";
		@Override
		public List<String> execute(Scanner scanner) throws BadCommand {
			portConfig config = get_port_config(scanner.next());
			config.setParity(scanner.next());
			return config.toStrings();
		}
	}
	class do_rtscts extends cliCommand
	{
		String help = "rtscts <portname> <yes,no>";
		@Override
		public List<String> execute(Scanner scanner) throws BadCommand {
			portConfig config = get_port_config(scanner.next());
			config.setFlowRTS(scanner.nextBoolean());
			return config.toStrings();
		}
	}
	class do_xonxoff extends cliCommand
	{
		String help = "xonxoff <portname> <yes,no>";
		@Override
		public List<String> execute(Scanner scanner) throws BadCommand {
			portConfig config = get_port_config(scanner.next());
			config.setFlowXONXOFF(scanner.nextBoolean());
			return config.toStrings();
		}
	}
	class do_sshport extends cliCommand
	{
		String help = "sshport <portname> <nnnn>";
		@Override
		public List<String> execute(Scanner scanner) throws BadCommand {
			portConfig config = get_port_config(scanner.next());
			config.setPort(scanner.nextInt());;
			return config.toStrings();
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
	cliCommands.put("commit", new do_commit());
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
				String command = scanner.next();
				logger.info("Command -> " + command);
				if ( cliCommands.containsKey(command)) 
				{
					try {
						String response = StringUtils.join(cliCommands.get(command).execute(scanner), "\r\n");
						logger.info("Response -> " + response);
						out.write(response.getBytes("UTF8"));
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
			out.flush();
			err.flush();
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
