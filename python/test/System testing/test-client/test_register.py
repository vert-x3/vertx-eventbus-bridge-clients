import Eventbus.Eventbus as Eventbus
import Eventbus.DeliveryOption as DeliveryOption
import json
import unittest
import time

#handlers (self,message)

class Client(unittest.TestCase):
	('System Testing')
	result={'msg':'test',}
	
	#Handler 
	def Handler(self,message):
		if message != None:
			Client.result=message['body']
			print(Client.result)
			
	def test_publish(self):
		c=Client()
		eb=Eventbus.Eventbus(self,'localhost', 7001)	

		eb.registerHandler('echo',Client.Handler)
		assert(eb.Handlers['echo']!=None)
		
		eb.unregisterHandler('echo')
		if eb.Handlers != None:
			assert(1)

		#close after 2 seconds
		eb.closeConnection(2)
			

if __name__ == '__main__':
		unittest.main()
