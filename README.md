# TCP-eventbus-client-Python

This is a TCP eventbus implementation for python clients. The protocol is quite simple:

* 4bytes int32 message length (big endian encoding)
* json string
* built-in keys

 1) type: (String, required) One of "send", "publish", "register", "unregister".
 
 2) headers: (Object, optional) Headers with JSON format. Value of string type is supported.
 
 3) body: (Object, optional) Message content in JSON format.
 
 4) address: (String, required) Destination address
 
 5) replyAddress: (String, optional) Address for replying to. 
 
 Download: !https://pypi.python.org/pypi/vertx-eventbus/1.0.0
 
 See wiki for more information.

example:

```python
    class Client():
	
	  #Handler
	    def Handler(self,message):
		    if message != None:
			    print(message['body']['result'],'4');

		eb=Eventbus.Eventbus(Client(),'localhost', 7000)	

		#jsonObject -body
		body={'msg':'add 4 to 0',}

		#DeliveryOption
		do=DeliveryOption.DeliveryOption();
		do.addHeader('type','text')
		do.addHeader('size','small')
		do.addReplyAddress('add')
		do.setTimeInterval(5) 

		#register handler
		eb.registerHandler('add',Client.Handler);

		#send 
		eb.send('add',body,do)

		#close after 5 seconds
		eb.closeConnection(5)
```

examples :
Simple example,
TimeKeeper




