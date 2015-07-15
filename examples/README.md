# Examples
After compiling and running both LejosWebApi and EchoReciever data can be sent to the robot using one of the example programs.

All examples follow the same pattern:

1. Establish a connection to the robot
2. Send the "Hello World" string
3. Reads the reply from the robot and displays it

## C example
* Compile with:

```
gcc c.c -o example
```

* Run with

```
./example
```

## Shell example using cURL
This program requires cURL

```
sudo apt-get install curl
```

* Run with

```
./curl.sh
```

## Golang example
* Run with

```
go run go.go
```

## Python example
This program requires the requests package 

```pip install requests```

* Run with

```
python python.py
```
