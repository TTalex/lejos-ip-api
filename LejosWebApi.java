/*
LejosWebApi is a hack allowing a Lego Mindstorm NXT robot to be IP enabled.
It consists of a webserver with a REST interface forwarding data to the robot via Bluetooth.
A connection has to be established first using the following request:
    HTTP Method     |    Path    |    Payload
        POST        |  /connect  | NXT MAC address

The following table shows the mapping between REST requests and data streams methods:
    HTTP Method     |    Path    |    Payload    |    Data Stream Method
        GET         |    /int    |               |        readInt()
      POST/PUT      |    /int    |       42      |       writeInt(42)
                    |            |               |
        GET         |    /utf    |               |        readUTF()
      POST/PUT      |    /utf    |  Hello World  |  writeUTF("Hello World")

In case of success an HTTP reply is sent with code 200 and the wanted payload for GET requests.
In case of failure an HTTP reply is sent with code >400 with error information in payload.
* Compile with:
nxjpcc LejosWebApi.java
* Run with:
nxjpc LejosWebApi
*/
import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.nio.ByteBuffer;

import lejos.pc.comm.NXTInfo;
import lejos.pc.comm.NXTComm;
import lejos.pc.comm.NXTCommFactory;
import lejos.pc.comm.NXTCommException;

import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;


class LejosWebApi{
    private static DataInputStream dis;
    private static DataOutputStream dos;

    public static void setDis(DataInputStream d){
        dis = d;
    }

    public static DataInputStream getDis(){
        return dis;
    }

    public static void setDos(DataOutputStream d){
        dos = d;
    }

    public static DataOutputStream getDos(){
        return dos;
    }
    
    public static void main(String[] args) throws Exception{
	HttpServer server = HttpServer.create(new InetSocketAddress(8082), 0);
	server.createContext("/connect", new ConnectHandler());
	server.createContext("/int", new IntHandler());
	server.createContext("/utf", new UtfHandler());
	//The executor allows for "unlimited" requests to be handled in parallel
	server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
	server.start();
    }
}


//ConnectHandler sets up a bluetooth connection to the NXT robot and initializes the data input and output streams that are used by the other functions.
class ConnectHandler implements HttpHandler {
    
    public void handle(HttpExchange t) throws IOException {
	byte[] buffer = new byte[1024];
	InputStream is = t.getRequestBody();
	OutputStream os = t.getResponseBody();
	int bytesread = is.read(buffer);
	String content = new String(buffer, 0, bytesread, "UTF-8");
	
	try{
	    NXTComm nxtComm = NXTCommFactory.createNXTComm(NXTCommFactory.BLUETOOTH);
	    NXTInfo nxtInfo = new NXTInfo(NXTCommFactory.BLUETOOTH, "NXT", content);

	    nxtComm.open(nxtInfo);
	    DataInputStream dis = new DataInputStream(nxtComm.getInputStream());
	    DataOutputStream dos = new DataOutputStream(nxtComm.getOutputStream());
	    LejosWebApi.setDis(dis);
	    LejosWebApi.setDos(dos);
	    t.sendResponseHeaders(200, 0);
	}catch (NXTCommException e){
	    String response = "Could not establish connection with NXT";
	    t.sendResponseHeaders(400, response.length());
	    os.write(response.getBytes());
	}
	t.close();
    }
}


//IntHandler is used to send and recieve Integers
class IntHandler implements HttpHandler {

    private void handlePost(HttpExchange t, OutputStream os) throws IOException{
	byte[] buffer = new byte[1024];
	InputStream is = t.getRequestBody();
	int bytesread = is.read(buffer);
	String content = new String(buffer, 0, bytesread, "UTF-8");
	int c = Integer.parseInt(content);
	System.out.println(c);
	try{
	    LejosWebApi.getDos().writeInt(c);
	    LejosWebApi.getDos().flush();
	    t.sendResponseHeaders(200, 0);
	}catch (NullPointerException e){
	    String response = "A connection has to be established first (see /connect)";
	    t.sendResponseHeaders(400, response.length());
	    os.write(response.getBytes());
	}
    }

    private void handleGet(HttpExchange t, OutputStream os) throws IOException{
	try{
	    int readint = LejosWebApi.getDis().readInt();
	    String response = Integer.toString(readint);
	    t.sendResponseHeaders(200, response.length());
	    os.write(response.getBytes());
	}catch (NullPointerException e){
	    String response = "A connection has to be established first (see /connect)";
	    t.sendResponseHeaders(400, response.length());
	    os.write(response.getBytes());
	}
    }

    public void handle(HttpExchange t) throws IOException {
	String method = t.getRequestMethod();
	OutputStream os = t.getResponseBody();
	if(method.equals("POST") || method.equals("PUT")){
	    handlePost(t, os);
	}else if(method.equals("GET")){
	    try{
		handleGet(t, os);
	    }catch (Exception e){
		System.out.println(e);
	    }
	}else{
	    String response = "Method Not Allowed";
	    t.sendResponseHeaders(405, response.length());
	    os.write(response.getBytes());
	}
	t.close();
    }
}


//UtfHandler is used to send and recieve UTF encoded strings
class UtfHandler implements HttpHandler {

    private void handlePost(HttpExchange t, OutputStream os) throws IOException{
	byte[] buffer = new byte[1024];
	InputStream is = t.getRequestBody();
	int bytesread = is.read(buffer);
	String content = new String(buffer, 0, bytesread, "UTF-8");
	try{
	    LejosWebApi.getDos().writeUTF(content);
	    LejosWebApi.getDos().flush();
	    t.sendResponseHeaders(200, 0);
	}catch (NullPointerException e){
	    String response = "A connection has to be established first (see /Connect)";
	    t.sendResponseHeaders(400, response.length());
	    os.write(response.getBytes());
	}
    }

    private void handleGet(HttpExchange t, OutputStream os) throws IOException{
	try{
	    String response = LejosWebApi.getDis().readUTF();
	    t.sendResponseHeaders(200, response.length());
	    os.write(response.getBytes());
	}catch (NullPointerException e){
	    String response = "A connection has to be established first (see /Connect)";
	    t.sendResponseHeaders(400, response.length());
	    os.write(response.getBytes());
	}
    }

    public void handle(HttpExchange t) throws IOException {
	String method = t.getRequestMethod();
	OutputStream os = t.getResponseBody();
	if(method.equals("POST") || method.equals("PUT")){
	    handlePost(t, os);
	}else if(method.equals("GET")){
	    handleGet(t, os);
	}else{
	    String response = "Method Not Allowed";
	    t.sendResponseHeaders(405, response.length());
	    os.write(response.getBytes());
	}
	t.close();
    }
}