#!/usr/bin/python
#Jayamine Alupotha

import socket
import json
import struct
import time
import types
import threading

#Eventbus constructor
#	input parameters
#		1) instance 
#		1) host	- String
#		2) port	- integer(>2^10-1)
#		3) TimeOut - float- receive TimeOut
#		4) TimeInterval -float -sleep time
#	inside parameters
#		1) socket
#		2) handlers - List<String address,array<functions/handler>> 
#		3) state -integer
#		4) ReplyHandler - <address,function>
#		5) writable - boolean {1: sendFrame, 0: receiving
#		
#Eventbus state
#	0 - not connected/failed
#	1 - connecting
#	2 - connected /open
#	3 - closing
#	4 - closed

class Eventbus:
	('TCP eventbus client for python')
	sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
	Handlers = {}
	state = 0
	ReplyHandler={}
	writable=True
	
	#constructor
	def __init__(self,instance, host='localhost', port=7000, TimeOut=0.1,TimeInterval=10.0):
		self.host = host
		self.port = port
		self.this = instance
		if TimeOut <0.01:
			self.TimeOut = 0.01
		else:
			self.TimeOut = TimeOut
			
		self.TimeInterval=TimeInterval
		#connect
		try:
			self.state = 1
			self.sock.connect((self.host, self.port))
			self.sock.settimeout(self.TimeOut)
			t1 = threading.Thread(target = self.receivingThread )
			t1.start()
			self.state = 2 
		except IOError as e:
			self.printErr(1,'SEVERE',str(e))
		except Exception as e:
			self.printErr('Undefined Error','SEVERE',str(e))
			
#Connection send and receive--------------------------------------------------------------------

	def isConnected(self):
		if self.state is 2:
			return True
		return False
	
	
	def sendFrame(self, message_s):
		message=message_s.encode('utf-8');
		frame = struct.pack('!I', len(message))+message
		self.sock.sendall(frame)
		
	def receive(self):
		try:
			if self.state <3:#closing socket
				len_str = Eventbus.sock.recv(4)
			else :
				return False
			len1 = struct.unpack("!i", len_str)[0]
			if self.state <3:#closing socket
				payload = Eventbus.sock.recv(len1)
			else: 
				return False
			json_message=payload.decode('utf-8');
			message=json.loads(json_message)
			#check
			#print(message)
			if message['type']== 'message':
				# failure message
				if 'address' not in message.keys():
					self.printErr('message failure', 'SEVERE', message)
				else:	
					try:
						#handlers		
						if self.Handlers[message['address']] != None:
							for handler in self.Handlers[message['address']]:
								handler(self.this,message)
							self.ReplyHandler=None
						
					except KeyError:	
						#replyHandler
						try:
							if self.ReplyHandler['address']== message['address']:
								self.ReplyHandler['replyHandler'](self.this,None,message)
								self.ReplyHandler=None
						except KeyError:
							print('no handlers for '+message['address'])
					
			elif message['type']=='err':
				try:
					self.ReplyHandler['replyHandler'](self.this,message,None)
					self.ReplyHandler=None
				except:
					pass
			else: 
				self.printErr(2,'SEVERE','Unknown type')
			return True
		except socket.timeout:
			return True
		except Exception as e:
			#1) close socket while thread is running
			#2) function error in the client code
			self.printErr('Undefined Error','SEVERE',str(e))
			return False
			#send error message
	
	
	
	def receivingThread(self):
		while self.state <3 : #0,1,2
			if self.writable == False:
				if self.receive()==False:
					break
				
	def closeConnection(self,timeInterval=30):
		if self.state==1:
			self.sock.close()
			return
		self.state=3
		time.sleep(timeInterval)
		try:
			self.sock.close()
		except:
			self.printErr('Undefined Error','SEVERE',str(e))
		self.state=4
	


#send, receive, register, unregister ------------------------------------------------------------
	
	#address-string
	#body - json object
	#deliveryOption - object
	#replyHandler -function
	
	def send(self, address, body=None, deliveryOption=None, replyHandler=None):
		if self.isConnected() is True:
			message=None
			
			if callable(deliveryOption)== True:
				replyHandler=deliveryOption
				deliveryOption=None
			
			if deliveryOption!= None:
				headers=deliveryOption.headers
				replyAddress=deliveryOption.replyAddress
				timeInterval=deliveryOption.timeInterval
			else :
				headers=None
				replyAddress=None
				timeInterval=self.TimeInterval
			
			message=json.dumps({'type':'send','address':address,'replyAddress':replyAddress,'headers':headers,'body':body,})
			#print('sent'+message)
			
			self.writable=True
			self.sendFrame(message)
			
			#replyHandler
			if replyAddress != None and replyHandler!=None:
				self.ReplyHandler={}
				self.ReplyHandler['address']=replyAddress
				self.ReplyHandler['replyHandler']=replyHandler
			self.writable=False	
			#time.sleep(timeInterval)
			i=0.0
			while timeInterval/self.TimeOut >= i:
				time.sleep(self.TimeOut)
				if self.ReplyHandler ==None: 
					break
				if timeInterval/self.TimeOut == i and self.ReplyHandler !=None:
					try:
						self.ReplyHandler['replyHandler'](self.this,'Time Out Error',None)
						self.ReplyHandler=None
						break
					except:
						break
				i+=1.0
		else:
			self.printErr(3,'SEVERE','INVALID_STATE_ERR')
			
	#address-string
	#body - json object
	#deliveryOption -object
	def publish(self,address,body,deliveryOption=None):
		
		if self.isConnected() is True:
			if deliveryOption!= None:
				headers=deliveryOption.headers
				replyAddress=deliveryOption.replyAddress
			else :
				headers=None
				replyAddress=None
				
			if replyAddress == None:
				message=json.dumps({'type':'send','address':address,'headers':headers,'body':body,})
			else:
				message=json.dumps({'type':'send','address':address,'headers':headers,'body':body,})
			
			self.writable=True
			self.sendFrame(message)
			self.writable=False
			
		else:
			self.printErr(3,'SEVERE','INVALID_STATE_ERR')
			
	#address-string
	#deliveryOption -object
	#replyHandler -function
	def registerHandler(self,address,handler):
		
		if self.isConnected() is True:
			message=None
			if callable(handler)== True:
				try:
					if (address not in self.Handlers.keys()) or (self.Handlers[address] == None):
						self.Handlers[address]=[]
						message=json.dumps({'type':'register','address':address,})
						self.writable=True
						self.sendFrame(message)
						self.writable=False
						time.sleep(self.TimeOut)
				except KeyError:
					self.Handlers[address]=[]
			
				try:
					self.Handlers[address].append(handler)
				except Exception as e:
					self.printErr(4,'SEVERE','Registration failed\n'+str(e))
			else:
				self.printErr(4,'SEVERE','Registration failed. Function is not callable\n')
		else:
			self.printErr(3,'SEVERE','INVALID_STATE_ERR')
		
		
	#address-string
	#deliveryOption -object
	#replyHandler -function
	def unregisterHandler(self,address):
		if self.isConnected() is True:
			message=None
			try:
				if self.Handlers[address]!= None:
					if len(self.Handlers) == 1:
						message=json.dumps({'type':'unregister','address':address,})
						self.sendFrame(message)
					del self.Handlers[address]
			except:
				self.printErr(5,'SEVERE','Unknown address:'+address)
				
		else:
			print('error occured: INVALID_STATE_ERR')
		
# Errors ------------------------------------------------------------------------------------------
	#1 - connection errors
	#2 - unknown type of the received message
	#3 - invalid state errors
	#4 - registration failed error
	#5 - unknown address of un-registeration
	def printErr(self,No,Category,error):
		print(No)
		print(Category)
		print (error)