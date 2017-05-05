import java.io.*;
import java.net.*;
import java.util.*;

public class P2Pclient {
	
	private static int serverID;
	private static InetAddress[] IPAddresses;
	private static boolean isReceiving;
	private static int[] serverPorts; 
	static File file;
	final static int SERVER1_PORT = 9860;
	static ThreadObject threadObj = new ThreadObject();
	static InetAddress server1_IPAddress; 
	
	public static void main(String argv[]) throws Exception{
	
		IPAddresses = new InetAddress[4];  
		BufferedReader inputFromUser = new BufferedReader(new InputStreamReader(System.in));
		DatagramSocket clientSocket = new DatagramSocket();
		
		//remember to change IP when using different computer
		server1_IPAddress = InetAddress.getByName("192.168.2.10");
		Scanner scanner = new Scanner(System.in);
		
		byte[] sendData = new byte[1024];
		byte[] receiveData = new byte[1024];
		
		//initialize packets
		DatagramPacket sendPacket = null;
		DatagramPacket receivePacket = null;
		
		// Streams for file receiving
		 InputStream istream = null;
         FileOutputStream fostream = null;
		
		//initialize client so that it knows IP's of remaining servers.
		init(server1_IPAddress, clientSocket, sendPacket, receivePacket);
		
		// Display Menu           
        System.out.println("Welcome to P2P client! :D");
        System.out.println("Initialization complete");
        boolean doExit = false;
        String contentName;
      //start thread to listen for other clients trying to start filetranser with us
		P2PListener listenerThread = new P2PListener();
		listenerThread.start();
        while (true)
        {
            System.out.println("Please choose from one of the following:");
            System.out.println("1) Inform and Update");
            System.out.println("2) Query file");
            System.out.println("3) Exit");
            int input = scanner.nextInt();
            System.out.println("-----------------------------");
        
			switch(input)
		    {
		    case 1:
		    	System.out.println("Please enter the content name:");
		    	contentName = scanner.next();
		    	informANDupdate(contentName, clientSocket, receivePacket, server1_IPAddress);
		    	break;
		    case 2:
		    	System.out.println("Please enter the content name:");
		    	contentName = scanner.next();
		    	// Ask for the existence of file from the DHT server
		    	boolean isFileFound = queryForContent(contentName, input, receivePacket, clientSocket, receivePacket);
		    	break;
		    case 3:
		    	doExit = true;
		    	break;
		    }
		    
		    if (doExit)
		    	break;

		
        }

}  
	
	static void init(InetAddress IPofDir1, DatagramSocket clientSocket, DatagramPacket sendPacket, DatagramPacket receivePacket) throws IOException
	{
		
		//uses IP address of Directory Server 1 to ask DHT for IP's of remaining servers
		byte[] sendData = new byte[1024];
		byte[] receiveData = new byte[1024];
		
		String requestRemIP = "request" ;
		sendData = requestRemIP.getBytes();
		sendPacket = new DatagramPacket(sendData, sendData.length, IPofDir1, SERVER1_PORT);
		clientSocket.send(sendPacket);
		System.out.printf("Sent request for IP's of remaining servers\n");
		
	    receivePacket = new DatagramPacket(receiveData, receiveData.length);
		clientSocket.receive(receivePacket);
		System.out.printf("Receive message containt IP's of remaining\n ");
		
		P2PMessage p2pMSG = new P2PMessage();
		byte[] data = receivePacket.getData();
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		ObjectInputStream is = new ObjectInputStream(in);
		
		try {
			p2pMSG = (P2PMessage)is.readObject();
			System.out.println("Received the msg from target Server"); 
		} catch (ClassNotFoundException e){
			System.out.println("Error");
			e.printStackTrace();
		}	
		// Copy IP address
		for (int i = 0; i < 3; i++)
			IPAddresses[i] = p2pMSG.IPAddresses[i];
	
	}	
	/**
	 * Get the server ID
	 * @param contentName the content name
	 * @return server ID
	 */
	public static int getServerID(String contentName){
		int hashed = 0;
		int sumAscii = 0; 
		contentName = splitString(contentName);
		
		//sum ascii
		for(int i = 0; i < contentName.length();i++)
			sumAscii += contentName.charAt(i); 
		System.out.println(sumAscii);
		
		//get hash
		hashed = (int) Math.floor(sumAscii%4);
		
		return hashed+1;
	}
		
	/**
	 * Contact the target server to store the record
	 * @param contentName the file name
	 * @param clientSocket the client socket
	 * @param sendPacket the datagram to send
	 * @param IPAddress the IP address
	 * @throws IOException 
	 */
	static void informANDupdate(String contentName,DatagramSocket clientSocket, DatagramPacket sendPacket, InetAddress IPAddress) throws IOException
	{
		//search our client directory for the content to see if it exists
		byte[] sendData = new byte[1024];
		file = new File(contentName);
		
		// Check if the file exist in the local directory (only for server!)
		if (!file.exists())
		{
			System.out.println("Content does not exist!");
			return;
		}
		
		System.out.printf("File %s exists\n", contentName);
		int serverID = getServerID(contentName);
		System.out.println("File is being updated on "+ serverID + " Server" );
		String record = "store " + serverID + " " + contentName;
	
		//Sending content record
		sendData = record.getBytes();	
		if(serverID==1){
			sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress,  SERVER1_PORT);
		}else{
			sendPacket = new DatagramPacket(sendData, sendData.length, IPAddresses[serverID-1],  SERVER1_PORT);
		}
		clientSocket.send(sendPacket);
		
		System.out.println("Client Record sent in Packet");
		System.out.printf("Server ID is:%d\n", serverID);
	}

	static int getServerID() { return serverID; }
	static void setServerID(int value) { serverID = value; }
	
	/**
	 * Splits content name from content name extension
	 * @param contentName the whole file name
	 * @return the content name (without the extension)
	 */
	static String splitString(String contentName)
	{
		String[] contents = new String[2];
		System.out.printf("content:%s\n", contentName);
		contents = contentName.split("\\.");
		return contents[0];		
	}

	/**
	 * Queries the directory server for content requested
	 * @param contentName name of file with extension
	 * @param serverID hashed server ID
	 * @param sendPacket packet for message sending
	 * @param clientSocket socket for message sending
	 * @param IPAddress to direct message to be sent
	 * @throws IOException
	 */
	static boolean queryForContent(String contentName, int serverID, DatagramPacket sendPacket, DatagramSocket clientSocket,DatagramPacket receivePacket) throws IOException
	{
		
		byte[] sendData = new byte[1024];
		byte[] receiveData = new byte[1024];
		String query = "query " + serverID + " " + contentName;
		splitString(contentName);
		serverID = getServerID(contentName);
		//Querying for content in DHT servers
		sendData = query.getBytes();
		if(serverID==1){
			sendPacket = new DatagramPacket(sendData, sendData.length, server1_IPAddress,  SERVER1_PORT);
		}else{
			sendPacket = new DatagramPacket(sendData, sendData.length, IPAddresses[serverID-1],  SERVER1_PORT);
		}
		clientSocket.send(sendPacket);
		System.out.printf("Quering for content...");
		
	    receivePacket = new DatagramPacket(receiveData, receiveData.length);
		clientSocket.receive(receivePacket);
		System.out.printf("Receiving response from server");
		
		P2PMessage p2pMSG = new P2PMessage();
		byte[] data = receivePacket.getData();
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		ObjectInputStream is = new ObjectInputStream(in);
		
		try {
			p2pMSG = (P2PMessage)is.readObject();
			System.out.println("Received the msg from target Server"); 
		}
		catch (ClassNotFoundException e){
			System.out.println("Error");
			e.printStackTrace();
		}	
		
		System.out.printf("Server response message: " + p2pMSG.found + "\n");
		//check for DHT response
		if (p2pMSG.found == 0)
		{
			System.out.println("ERROR 404: Content Not Found!");
			return false;
		}
		p2pMSG.content = contentName;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(outputStream);
		os.writeObject(p2pMSG);
		data = outputStream.toByteArray();
		sendPacket = new DatagramPacket(data, data.length, p2pMSG.contentIP , 9880);
		clientSocket.send(sendPacket);
		System.out.println("Request sent to P2P Client"); 
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Socket p2pSocket = new Socket(p2pMSG.contentIP, 40021);
		System.out.println("P2P TCP Connection established");
       
		byte[] bytes = new byte[1024];
      
           InputStream istream = p2pSocket.getInputStream();
           FileOutputStream fostream = new FileOutputStream("C:// "+ contentName);

            int segment = istream.read(bytes);
            while (segment >= 0) {
                fostream.write(bytes, 0, segment);
                segment = istream.read(bytes);
            }
        fostream.close();
		System.out.println("Received Immage , Transfer Complete!");
		p2pSocket.close();
		return true;
	}
	
}