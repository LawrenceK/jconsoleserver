package org.klyne.consoleserver;
//import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.io.IOException;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;

public class ConsoleServer {

	public static void main(String[] args) {
//		Security.addProvider(new BouncyCastleProvider());
		
		ConsoleServer cServer = new ConsoleServer();
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
	}
	HashMap<String,sshServer> sshServers = new HashMap<String,sshServer>();
	
	public ConsoleServer()
	{
		
	}
	public void start()
	{
		portConfig config;
		for ( int index = 0; ; index++)
		{
			try {
				config = new portConfig("/dev/ttyUSB" + index, 8000+index);
				File f = new File(config.getName()); 
				if ( !f.exists() )
				{
					break;
				}
				sshServers.put(config.getName(), new sshServer(config) );
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
