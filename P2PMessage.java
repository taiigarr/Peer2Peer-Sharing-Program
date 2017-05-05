	import java.io.*;
import java.net.*;
	
@SuppressWarnings("serial")
public class P2PMessage implements Serializable{
	String sender;
	String receiver; 
	int serverID; 
	int found;
	String command; 
	String content;
	boolean response;
	int  clientPort; 
	InetAddress clientIP; 
	int[] serverPorts;
	InetAddress contentIP;
	InetAddress[] IPAddresses = new InetAddress[3];
}
