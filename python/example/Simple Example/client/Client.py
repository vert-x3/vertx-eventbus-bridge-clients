import eventbus.Eventbus as Eventbus
import eventbus.DeliveryOption as DeliveryOption
import json

#replyHanlder (self,error,message)
#handlers (self,message)

class Client:

	#replyHandler 
	def replyhandler(self,error,message):
		if error != None:
			print(error)
		if message != None:
			print(message['body'])
	
	#Handler for errors and msg
	def Handler(self,message):
		if message != None:
			print(message['body'])
			
	
eb=Eventbus.Eventbus('localhost', 7000)	

#jsonObject -body
body={'msg':'hi server!',}

#DeliveryOption
do=DeliveryOption.DeliveryOption();
do.addHeader('type','text')
do.addHeader('size','small')
do.addReplyAddress('welcome')
do.setTimeInterval(1) 

#register handler
eb.registerHandler('welcome',Client.Handler);

#send 
eb.send('welcome',body,do)

#send
eb.send('ignore',body,do,Client.replyhandler)

#close after 5 seconds
eb.closeConnection(5)