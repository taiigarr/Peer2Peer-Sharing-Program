import java.net.InetAddress;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class DHTloop extends Thread{
	public InetAddress[] ServerIP= new InetAddress[3];
	public InetAddress fServer; 
	public int numServer; 
	final static int SERVER2SERVERPORT = 40020; 
	
	int thisServerID ;
	int nextServerID ;  
	InetAddress IPAddress; 
	int num;
	InetAddress[] IPAddresses;
	Socket toSocket;
	String[] listOfContent;
	InetAddress[] listOfIP;
	int  maxIndexOfContent;
	int DHTPORT;
	BufferedReader inFromServer;
	public DHTloop(int DHTPORT ,int thisServerID , int nextServerID, InetAddress IPAddress ,  InetAddress[] IPAddresses, String[] listOfContent, InetAddress[] listOfIP, int  maxIndexOfContent){
		this.IPAddresses = new InetAddress[3];
		this.thisServerID = thisServerID ;
		this.nextServerID = nextServerID ;  
		this.IPAddress = IPAddress; 
		this.IPAddresses[0]= IPAddresses[0];
		this.IPAddresses[1]= IPAddresses[1];
		this.IPAddresses[2]= IPAddresses[2];
		this.listOfContent= new String[50];
		this.listOfIP= new InetAddress[50];
		this.maxIndexOfContent = 0;
		this.DHTPORT= DHTPORT;
	}
	
	@SuppressWarnings("resource")
	public void run(){
		System.out.println("Thread : Creating socket");	
		ServerSocket fromSocket = null;
		
		DatagramSocket serverSocket = null;
		try {
			serverSocket = new DatagramSocket(DHTPORT-1);
		} catch (SocketException e4) {
			// TODO Auto-generated catch block
			e4.printStackTrace();
		}
		
		Socket previousLink = null; 
		try {
			fromSocket = new ServerSocket(DHTPORT);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		try {
			previousLink = fromSocket.accept();
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		System.out.println("Thread : Successful Link to loop");
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}
		
		ObjectInputStream inFromServer = null;
		try {
			 inFromServer = new ObjectInputStream(previousLink.getInputStream());
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		

		
		System.out.println("Thread : Starting up listener for server ...");
		
		
		
		//Receiving / Listening 
		

		while(true){
			P2PMessage receivedMessage = new P2PMessage();
			ObjectOutputStream outToServer = null;
			
			//receivedMessage;
			try {
				receivedMessage = (P2PMessage)inFromServer.readObject();
			} catch (ClassNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			System.out.println("Thread: Recieved request from other server"); 

			//Checking conditions 
			if(receivedMessage.command.contains("query")){
				//If the message has gone through the loop
				System.out.println("Thread : Command - Query ");
				if(thisServerID == receivedMessage.serverID){
					if(receivedMessage.found == 0 ){
						System.out.println("Thread : Content not found, returning Error");
						P2PMessage p2pMSG=new P2PMessage();
						p2pMSG.command = "NotFound";
						ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
						ObjectOutputStream os = null;
						try {
							os = new ObjectOutputStream(outputStream);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						try {
							os.writeObject(p2pMSG);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						byte[] senddata = outputStream.toByteArray();
						
						DatagramPacket sendPacket = new DatagramPacket(senddata, senddata.length, receivedMessage.clientIP, receivedMessage.clientPort );
						try {
							serverSocket.send(sendPacket);
							//outToServer.writeObject(p2pMSG);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						System.out.println("Thread : Sent Message to client");
					}else{
						//Send msg to client with found IP 
						System.out.println("Thread : Content was found, Sending message to Client");
						receivedMessage.command = "Found";
						ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
						ObjectOutputStream os = null;
						try {
							os = new ObjectOutputStream(outputStream);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						try {
							os.writeObject(receivedMessage);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						byte[] senddata = outputStream.toByteArray();
						DatagramPacket sendPacket = new DatagramPacket(senddata, senddata.length, receivedMessage.clientIP, receivedMessage.clientPort );
						try {
							serverSocket.send(sendPacket);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						System.out.println("Sent Message to client");
					}
					}else{
					int key = -1; 
					System.out.println("Thread : Checking server ...");
					for(int index = 0 ; index < maxIndexOfContent  ; index++ ){
						if(listOfContent[index].contains(receivedMessage.content)){
							key = index ; 
						}
					}
					if(key == -1){
						System.out.println("Thread : Content was not found on this server, searching next");
						try {
							outToServer = new ObjectOutputStream(toSocket.getOutputStream());
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						try {
							outToServer.writeObject(receivedMessage);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if(nextServerID==0){
							System.out.println("Thread : Sending to DHT server 1");
						}
						else{
							System.out.println("Thread : Sending to DHT server "+nextServerID);
						}

						
						System.out.println("Thread : Sent MSG to DHT loop querying for content");
					}
					else{
						System.out.println("Thead : Content was found!, Sending results through loop");
						receivedMessage.contentIP = listOfIP[key];
						if(nextServerID==0){
							System.out.println("Thread : Sending to DHT server 1");
						}
						else{
							System.out.println("Thread : Sending to DHT server "+nextServerID);
						}
						
						try {
							outToServer.writeObject(receivedMessage);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						System.out.println ("Sent through the loop of DHT");
				}
			}
		
		}
		}
	}
}

