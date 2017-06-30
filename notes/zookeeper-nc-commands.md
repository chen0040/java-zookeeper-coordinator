


```bash
echo srvr |nc localhost 2181
echo stat | nc 127.0.0.1 2181
```

or
 
```bash
echo srvr |nc zoo1 2181
echo stat | nc zoo1 2181
```

srvr is new in 3.3.0, it provides basic details on the running server
stat provides details detail similar to “srvr” however it also includes a summary of connection information (the clients).

Link: https://phunt1.wordpress.com/
