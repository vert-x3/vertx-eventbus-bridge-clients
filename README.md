# TCP-eventbus-client-C
This is a TCP eventbus implementation for C clients. The protocol is quite simple:

* 4bytes int32 message length (big endian encoding)
* json string
* built-in keys
        
        1) type: (String, required) One of "send", "publish", "register", "unregister".
        
        2) headers: (Object, optional) Headers with JSON format.
        
        3) body: (Object, optional) Message content in JSON format.
        
        4) address: (String, required) Destination address
        
        5) replyAddress: (String, optional) Address for replying to.
        

example:

```c



```
