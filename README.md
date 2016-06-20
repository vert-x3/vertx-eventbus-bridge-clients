# TCP-eventbus-client-Python
This is a TCP eventbus implementation for python clients. The protocol is quite simple:

* 4bytes int32 message length (big endian encoding)
* json string
* built-in keys
    *type: (String, required) One of "send", "publish", "register", "unregister".
    *headers: (Object, optional) Headers with JSON format. Value of string type is supported.
    *body: (Object, optional) Message content in JSON format.
    *address: (String, required) Destination address
    *replyAddress: (String, optional) Address for replying to. 

example:

    class Client():
	
	  #Handler for errors and msg
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
		eb.registerHandler('add',do,Client.Handler);

		#send 
		eb.send('add',body,do)

		#close after 5 seconds
		eb.closeConnection(5)


examples :
Simple example,
TimeKeeper




