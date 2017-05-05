import java.io.*;
import java.util.HashMap;
import java.util.concurrent.*;
import java.net.*;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.IOException;

public class ClientListener extends Thread{
	
	DatagramSocket serverSocket;
	byte[] receiveData = new byte[1024];
	DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
	
	public ClientListener(HashMap<String, InetAddress> map) throws Exception{
	 serverSocket = new DatagramSocket(9876);
	}
	
	@Override
	public void run() {
		try {
			serverSocket.receive(receivePacket);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		byte[] data = receivePacket.getData();
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		ObjectInputStream is = null;
		try {
			is = new ObjectInputStream(in);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		P2PMessage p2pMSG = new P2PMessage(); 
		try {
			try {
				p2pMSG = (P2PMessage)is.readObject();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Received message from client"); 
		} catch (ClassNotFoundException e){
			System.out.println("Error");
			e.printStackTrace();
		}
		
		
		
	}

}
