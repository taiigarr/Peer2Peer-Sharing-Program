import java.io.*;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.*;
import java.net.*;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.io.IOException;
public class DHTserver {
	final static int NumOfServer=1;
	static String[] listOfContent= new String[50];
	static InetAddress[] listOfIP= new InetAddress[50];
	static int[] listOfPorts= new int[50];
	static int  maxIndexOfContent = 0; 
	final static int SERVERPORT = 9860; 
	final static int DHTPORT = 40020;
	final static int SERVER2SERVERPORT = 9400; 
	static InetAddress[] IPAddresses ;
 	static int thisServerID = 0; 
 	static int nextServerID = 0 ; 
	
	public static void main(String argv[])throws Exception{
		//Means the client has started up :) 
		listOfContent= new String[50];
		listOfIP= new InetAddress[50];
		listOfPorts= new int[50];
		System.out.println("Successful Start Up !");
		//Default server 1 Socket
		//put
		//map.put(FileName, IP )
		DatagramSocket serverSocket = new DatagramSocket(SERVERPORT);
		IPAddresses = new InetAddress[3];
		//Data 
		byte[] receiveData = new byte[1024];
		byte[] sendData = new byte [1024];
		//MODIFIY IF MAIN SERVER
		InetAddress IPAddress =InetAddress.getByName("192.168.2.10");
		//Receive input if first Server
		BufferedReader inputFromUser = new BufferedReader(new InputStreamReader(System.in));
		//Just to know if first server
		System.out.println("Is this first server?");
		String firstServer = inputFromUser.readLine();
		if(firstServer.equalsIgnoreCase("yes")){
			thisServerID = 0; 
			firstServer(receiveData, sendData, serverSocket);
		}
		else{
			otherServer(receiveData,sendData,serverSocket,IPAddress);
		}
		//Received the InetAddresses
		//Intilize UDP Listen from client
		//Buffer for other servers
		Socket toSocket = null;
		//System.out.println("Creating socket");	
		
		DHTloop listen = new DHTloop(DHTPORT , thisServerID ,nextServerID, IPAddress , IPAddresses, listOfContent,listOfIP,maxIndexOfContent);
		listen.start();
		Thread.sleep(3000);
		
		System.out.println("Connecting socket");
		if(thisServerID + 1 > NumOfServer ){
			nextServerID = 0 ; 
		}
		else 
			nextServerID=thisServerID+1; 
		System.out.println("Next Server in loop is " + nextServerID ); 
		if(nextServerID == 0){
			toSocket = new Socket(IPAddress, DHTPORT);
		}
		else{
		System.out.println("ID of Server to connect to " + IPAddresses[nextServerID-1].getHostAddress() );
		toSocket = new Socket(IPAddresses[nextServerID-1],DHTPORT);
		}
		listen.toSocket = toSocket ;
		System.out.println("Delaying for Sync");
		Thread.sleep(1000);
		
		//Initilization of buffers for send and recieve
		DataOutputStream outToserver = new DataOutputStream(toSocket.getOutputStream());
		Socket previousLink = null;
		System.out.println("Set up for buffers for DHT ring complete");
		//Spawning Thread 
		System.out.println("IPS: " + IPAddresses[0].getHostAddress());

		
		
		Scanner divider;
		//check what is received from client
		while(true){
			receiveData = new byte[1024];
			System.out.println("Waiting for P2P Client requests.... ");
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			serverSocket.receive(receivePacket);
			System.out.println("Request has been receieved");
			String message = new String(receivePacket.getData());
			System.out.println("Message Recieved : " + message);
			divider = new Scanner(message);
			String demands = divider.next(); 
			System.out.println("Demand from client : " + demands);
			if(demands.contains("request")){ 
				System.out.println("Creating P2P Message ...");
				P2PMessage p2pMSG = new P2PMessage();
				for (int i = 0 ; i< 3 ; i++){
					p2pMSG.IPAddresses[i] = IPAddresses[i];
				}
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				ObjectOutputStream os = new ObjectOutputStream(outputStream);
				os.writeObject(p2pMSG);
				byte[] data = outputStream.toByteArray();
				DatagramPacket sendPacket = new DatagramPacket(data, data.length, receivePacket.getAddress(), receivePacket.getPort());
				serverSocket.send(sendPacket);
				System.out.println("Sent IP Addresses of DHT Loop");
			}
			else if(demands.contains("store")){
				System.out.println("Linking information to DB");
				System.out.println("Please hold ... WUBWUBWUBW");
				divider.next();
				String content = divider.next();
				System.out.println("Divider : " + content );
				listOfContent[maxIndexOfContent]=content;
				listOfIP[maxIndexOfContent]=receivePacket.getAddress();
				listen.listOfContent[maxIndexOfContent]=content;
				listen.listOfIP[maxIndexOfContent]=receivePacket.getAddress();
				System.out.println("Content name : " + listOfContent[maxIndexOfContent]);
				System.out.println("IP Address of Content : " + listOfIP[maxIndexOfContent].getHostAddress());
				maxIndexOfContent++; 
				listen.maxIndexOfContent++; 
			}
			else if(demands.contains("query")){
				System.out.println("Searching Current DB for content");
				divider.next();
				String content = divider.next();
				System.out.println("Content Name : " + content );
				int key = -1; 
				for(int index = 0 ; index < maxIndexOfContent ; index++){
					if(listOfContent[index].contains(content)){
						key = index; 
					}
				}
				if(key==-1){
					System.out.println("Content was not found in this server, Cheching loop...");
					P2PMessage checkServers = new P2PMessage();
					checkServers.serverID = thisServerID; 
					checkServers.command = "query"; 
					checkServers.content = content; 
					checkServers.clientIP = receivePacket.getAddress();
					checkServers.clientPort= receivePacket.getPort();
					// Makes a message for the next server but keeps the ID of this server so we know it full looped. 
					
					ObjectOutputStream outToServer = new ObjectOutputStream(toSocket.getOutputStream());
					outToServer.writeObject(checkServers);
					outToServer.flush();
			
					
					
				}
				else{
				System.out.println("Content was found, Sending message to Client");
				P2PMessage p2pMSG = new P2PMessage();
				p2pMSG.serverID = thisServerID;
				p2pMSG.found = 1 ; 
				p2pMSG.contentIP = listOfIP[key];
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				ObjectOutputStream os = new ObjectOutputStream(outputStream);
				os.writeObject(p2pMSG);
				byte[] data = outputStream.toByteArray();
				DatagramPacket sendPacket = new DatagramPacket(data, data.length, receivePacket.getAddress(), receivePacket.getPort());
				serverSocket.send(sendPacket);
				System.out.println("Sent Message to Client");
				}
			}
			
		}
		
		//Intilize TCP Listen
		//Linking to TCP socket of the loop
		
		
	}
		//Intilization of servers if First server
		static void firstServer(byte[] receiveData, byte[] sendData, DatagramSocket serverSocket) throws IOException
		{
			System.out.println("Running FirstServer method");
			//InetAddress[] IPAddresses = new InetAddress[3]; 
			int index=0;
			P2PMessage p2pMSG = new P2PMessage();
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			while (index < NumOfServer)
			{//Receives a message from another server 
				serverSocket.receive(receivePacket);
				String message = new String(receivePacket.getData());
				//gets message and compares msg ifServer , if so stores IP
				System.out.println("Received message");
				System.out.println("Message : " + message);
				//If message is server
				if(message.charAt(0) == 's'){
					System.out.println("Comfirmed, Message from Server.");
					InetAddress IPAddress = receivePacket.getAddress();
					System.out.println("Server " + index + " has been added with IP " + IPAddress );
					IPAddresses[index]=IPAddress;
					p2pMSG.IPAddresses[index]=IPAddress;
					index++; 
				}
			}
			//Sending object through the pipe , pipe , pipe , pipe
			for(int i = 0; i < NumOfServer ;i++){
				p2pMSG.serverID = i+1; 
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				ObjectOutputStream os = new ObjectOutputStream(outputStream);
				os.writeObject(p2pMSG);
				byte[] data = outputStream.toByteArray();
				DatagramPacket sendPacket = new DatagramPacket(data, data.length, p2pMSG.IPAddresses[i], SERVERPORT );
				serverSocket.send(sendPacket);
				System.out.println("First server has sent the listtttttt to "+ i + " sever!!!");
			}
			
		}
		//Intilization of servers if NOT first server
		static void otherServer(byte[] receiveData, byte[] sendData, DatagramSocket serverSocket,InetAddress IPAddress) throws IOException
		{
			//Sends the first server a message that it is a server
			String sentence = "server";
			sendData = sentence.getBytes();
			P2PMessage p2pMSG = new P2PMessage();
			DatagramPacket sendPacket = new DatagramPacket(sendData,sendData.length,IPAddress,SERVERPORT);
			serverSocket.send(sendPacket);
			System.out.println("Sever has sent intilization the 1st Server");
			
			
			DatagramPacket incomingPacket = new DatagramPacket(receiveData, receiveData.length);
			serverSocket.receive(incomingPacket);
			byte[] data = incomingPacket.getData();
			ByteArrayInputStream in = new ByteArrayInputStream(data);
			ObjectInputStream is = new ObjectInputStream(in);
			try {
				p2pMSG = (P2PMessage)is.readObject();
				System.out.println("Received the IP array from 1st Server"); 
			} catch (ClassNotFoundException e){
				System.out.println("Error");
				e.printStackTrace();
			}
			IPAddresses[0]= p2pMSG.IPAddresses[0];
			IPAddresses[1]= p2pMSG.IPAddresses[1];
			IPAddresses[2]= p2pMSG.IPAddresses[2];
			thisServerID = p2pMSG.serverID; 
			System.out.println("ServerID : "+ thisServerID);
			
		}
	
	}

	


