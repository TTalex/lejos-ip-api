/*
EchoReciever is a simple echo program for the Lego Mindstorm NXT robot using the lejos firmware. 
After establishing a Bluetooth connection, it listens for UTF encoded messages and echos them.
* Compile with:
nxjc EchoReciever.java
* Run with:
nxj -r -o EchoReciever.nxj EchoReciever
*/
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import lejos.nxt.Sound;
import lejos.nxt.LCD;
import lejos.nxt.Button;
import lejos.nxt.ButtonListener;
import lejos.nxt.comm.NXTConnection;
import lejos.nxt.comm.Bluetooth;


class EchoReciever{
    private static DataInputStream dis;
    private static DataOutputStream dos;
   
    protected static void stopall(){
	try{
	    dis.close();
	    dos.close();
	}catch (IOException e){
	    //Streams are already closed
	}
	System.exit(0);

    }
    public static void main(String[] args){
	//Exit the program when the ESCAPE button is pressed
	Button.ESCAPE.addButtonListener(new ButtonListener(){
		public void buttonPressed(Button b){
			stopall();
		}
		public void buttonReleased(Button b){
		}
	    });


	//Wait for a bluetooth connection
	LCD.drawString("Waiting ..", 0, 0);
	NXTConnection connection = Bluetooth.waitForConnection();
	dis = connection.openDataInputStream();
	dos = connection.openDataOutputStream();

	LCD.clear();
	LCD.drawString("Connected", 0, 0);

	//Play a sound to when the device is connected
	int volume = Sound.getVolume();
	Sound.setVolume(50);
	Sound.playNote(Sound.PIANO, 440, 100);
	Sound.setVolume(volume);

	String s;
	while(true){
	    LCD.drawString("Waiting for UTF", 0, 1);
	    try{
		s = dis.readUTF();
		LCD.clear();
		LCD.drawString(s, 0, 2);
		
		dos.writeUTF(s);
		dos.flush();
	    }catch (IOException e){
		// The program is being closed
	    }
	}
    }
}
