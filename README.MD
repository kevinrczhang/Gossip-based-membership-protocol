To test this software, first run:
<br />
```
javac -d ../out src/config/*.java src/remote/*.java src/service/*.java src/node/*.java

```
In your terminal to compile all the necessary java classes. Then, run 
```
java -cp ../out remote.Main 127.0.0.1 8081 127.0.0.1:8080
```
To specify the address and portname of the initial node, and `none` for the second node
(since this is just the initial node we are adding, no other node is currently in the system)
<br />
This should then start up a new node, and you will start seeing output for it. 
<br />
To add another node, open a new terminal and run
```
java remote.Main 127.0.0.1 <new_port_name> <target_address_and_portnumber>
```
This will then start up a new node instance that points the existing node as a target instance,
and they will then begin communicating
<br />
You can repeat this process to add as many nodes as you please. 
