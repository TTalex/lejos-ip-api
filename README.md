# lejos-ip-api
lejos-ip-api is a hack allowing a Lego Mindstorm NXT robot to be IP enabled.

It is compiled using the [Lejos nxj framework](http://www.lejos.org/nxj.php) and firmware (v0.9.1).

A connection has to be established first using the following request:

HTTP Method     |    Path    |    Payload
----------------|------------|---------------
    POST        |  /connect  | NXT MAC address
        
The following table shows the mapping between REST requests and data streams methods:

HTTP Method     |    Path    |    Payload    |    Data Stream Method
----------------|------------|---------------|-------------------------
    GET         |    /int    |               |        readInt()
  POST/PUT      |    /int    |       42      |       writeInt(42)
                |            |               |
    GET         |    /utf    |               |        readUTF()
  POST/PUT      |    /utf    |  Hello World  |  writeUTF("Hello World")
      
In case of success an HTTP reply is sent with code 200 and the wanted payload for GET requests.

In case of failure an HTTP reply is sent with code >400 with error information in payload.

## LejosWebApi.java
[LejosWebApi](https://github.com/TTalex/lejos-ip-api/blob/master/LejosWebApi.java) consists of a webserver with a REST interface forwarding data to the robot via Bluetooth.

* Compile with:

```sh
nxjpcc LejosWebApi.java
```

* Run with:

```sh
nxjpc LejosWebApi
```

## EchoReciever.java
[EchoReciever](https://github.com/TTalex/lejos-ip-api/blob/master/EchoReciever.java) is a simple echo program for the Lego Mindstorm NXT robot using the lejos firmware. 

After establishing a Bluetooth connection, it listens for UTF encoded messages and echos them.

* Compile with:

```sh
nxjc EchoReciever.java
```

* Run with:

```sh
nxj -r -o EchoReciever.nxj EchoReciever
```

## Examples
After compiling and running both LejosWebApi and EchoReciever, data can be sent to the robot using one of the example programs from the [examples](https://github.com/TTalex/lejos-ip-api/tree/master/examples) folder.
##Useful links
* [Pairing the NXT robot on linux](http://www.eggwall.com/2011/07/setting-up-nxt-bluetooth-support-on.html)
* [The Lejos NXJ Tutorial](http://www.lejos.org/nxt/nxj/tutorial/index.htm)
