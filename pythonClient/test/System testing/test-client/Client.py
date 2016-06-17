import Eventbus.Eventbus as Eventbus
import Eventbus.DeliveryOption as DeliveryOption
import json
import unittest

#replyHanlder (self,error,message)
#handlers (self,message)

class Client(unittest.TestCase):
	('System Testing')
	result='0'
	
	#Handler for errors and msg
	def Handler(self,message):
		if message != None:
			print(message['body'])
			self.assertEqual(message['body']['result'],'4');
			
	def test(self):
		c=Client()
		eb=Eventbus.Eventbus(self,'localhost', 7000)	

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
		eb.closeConnection(1)

if __name__ == '__main__':
		unittest.main()
