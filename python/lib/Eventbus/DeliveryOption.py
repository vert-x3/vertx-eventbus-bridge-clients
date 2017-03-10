import json

#DeliveryOption constructor
#	inside parameters
#		1) replyAddress
#		2) headers
#		3) timeInterval for reply

class DeliveryOption:
	('deliveryOption class describe headers and replyAddress')
	replyAddress = None
	headers = {}
	timeInterval=10.0
	
	def addHeader(self, header, value):
		self.headers[header]=value
	
	def deleteHeader(self, header):
		del self.headers[header]
		
	def addReplyAddress(self, replyAddress):
		self.replyAddress = replyAddress
		
	def deleteReplyAddress(self):
		self.replyAddress = None
	
	def setTimeInterval(self,time):
		self.timeInterval=time