import java.net.*;
import java.io.*;
import java.io.*;
import java.util.Scanner;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.io.IOException;

public class P2PListener extends Thread {

	
	P2PMessage p2pMSG;
	Socket TCPSocket;
	BufferedReader inputFromClient;
	String input ;
	FileInputStream fistream;
	BufferedInputStream bistream;
	ThreadObject obj;
	File file;
	InetAddress address;
	int port;
	OutputStream ostream = null;
	
	/**
	 * Listen for another peer to query for file. Send the file to the querying peer.
	 * @param param object containing the IP address, port number, and the file to send
	 * @throws Exception socket availability
	 */
	@SuppressWarnings("resource")
	public P2PListener() throws Exception
	{
	}
	@Override
	public void run() {
		//create TCP listener socket 
		//create file input stream
		//create buffered reader for binary version of file
		//convert file from bytes to file
		//initiate file transfer
		DatagramSocket serverSocket = null;
		ServerSocket listenerSocket = null;
		try {
			listenerSocket = new ServerSocket (40021);
		} catch (IOException e4) {
			// TODO Auto-generated catch block
			e4.printStackTrace();
		}
		try {
			serverSocket = new DatagramSocket(9880);
		} catch (SocketException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}
		
		System.out.println("Thread has Successfully started");
		while (true)
		{
			try {
				byte[] receiveData = new byte[1024];
				DatagramPacket incomingPacket = new DatagramPacket(receiveData, receiveData.length);
				serverSocket.receive(incomingPacket);
				byte[] data = incomingPacket.getData();
				ByteArrayInputStream in = new ByteArrayInputStream(data);
				ObjectInputStream is = new ObjectInputStream(in);
				try {
					p2pMSG = (P2PMessage)is.readObject();
					System.out.println("Thread: Received message from P2P Client"); 
				} catch (ClassNotFoundException e){
					System.out.println("Error");
					e.printStackTrace();
				}
				
			  TCPSocket = listenerSocket.accept();
			  System.out.println("Accepted connection : " + TCPSocket);
			  // send file
			  System.out.println("Content name : "+p2pMSG.content);
			  file = new File(System.getProperty("user.dir")+"//" + p2pMSG.content);
			  byte [] bytes  = new byte [(int)file.length()];
			  fistream = new FileInputStream(file);
			  bistream = new BufferedInputStream(fistream);
			  bistream.read(bytes,0, bytes.length);
			  ostream = TCPSocket.getOutputStream();
			  System.out.println("Sending " + file.getName() + "(" + bytes.length + " bytes)");
			  ostream.write(bytes,0,bytes.length);
			  ostream.flush();
			  System.out.println("File transfer successful");
			  
			} 
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finally 
			{
	          if (bistream != null)
				try {
					bistream.close();
				} catch (IOException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
	          if (ostream != null)
				try {
					ostream.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
	          if (TCPSocket!=null)
				try {
					TCPSocket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    }
			
		}
	}

}
