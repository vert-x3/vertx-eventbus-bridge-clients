#!/usr/bin/python
#This the library for python

import socket
import json
import struct
import time
import types
import threading

#Eventbus constructor
#	input parameters
#		1) host	- String
#		2) port	- integer(>2^10-1)
#		3) TimeOut - float- receive TimeOut
#		
#	inside parameters
#		1) socket
#		2) handlers - List<String address,array<functions/handler>> 
#		3) state -integer
#		4) ReplyHandler - <address,function>
#		5) socket_is_using - boolean {1: sendFrame, 0: receiving
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
	socket_is_using=True
	safe_close=True
	
	#constructor
	def __init__(self, host='localhost', port=7000, TimeOut=3.0,TimeInterval=0.1):
		self.host = host
		self.port = port
		if TimeOut <0.1:
			self.TimeOut = 0.1
		else:
			self.TimeOut = TimeOut
		
		#connect
		try:
			self.state = 1
			self.sock.connect((self.host, self.port))
			self.sock.settimeout(self.TimeOut)
			t1 = threading.Thread(target = self.receivingThread )
			t1.start()
			self.state = 2 
		except IOError as e:
			print( str(e))
		except Exception as e:
			print( str(e))
			
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
				#handlers
				try:
					if self.Handlers[message['address']] != None:
						for handler in self.Handlers[message['address']]:
							handler(self,message)
					
				except KeyError:	
					#replyHandler
					try:
						if self.ReplyHandler['address']== message['address']:
							self.ReplyHandler['replyHandler'](self,None,message)
							del self.ReplyHandler['address']
							del self.ReplyHandler['replyHandler']
					except KeyError:		
						#edit to pass error
						print('no handlers for '+message['address'])
					
			elif message['type']=='err':
				try:
					self.ReplyHandler['replyHandler'](self,message,None)
					del self.ReplyHandler['address']
					del self.ReplyHandler['replyHandler']
				except:
					pass
			#else: unknown type
				
			return True
		except socket.timeout:
			return True
		except Exception as e:
			#1) close socket while thread is running
			#2) function error in the client code
			print (str(e))
			return False
			#send error message
	
	
	
	def receivingThread(self):
		while self.state <3 : #0,1,2
			if self.socket_is_using == False:
				if self.receive()==False:
					break
				
	def closeConnection(self,timeInterval=30):
		self.state=3
		time.sleep(timeInterval)
		self.sock.close()
		self.state=4
	


#send, receive, register, unregister ------------------------------------------------------------

	def __result(self,state):
		if state==False:
			print('error occured:could not send')
	
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
				timeInterval=0.01
			
			message=json.dumps({'type':'send','address':address,'replyAddress':replyAddress,'headers':headers,'body':body,})
			#print('sent'+message)
			
			self.socket_is_using=True
			self.sendFrame(message)
			self.socket_is_using=False
			
			#replyHandler
			if replyAddress != None and replyHandler!=None:
				self.ReplyHandler['address']=replyAddress
				self.ReplyHandler['replyHandler']=replyHandler
				
			time.sleep(timeInterval)
		else:
			print('error occured: INVALID_STATE_ERR')
			
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
				message=json.dumps({'type':'send','address':address,'replyAddress':address,'headers':headers,'body':body,})
			
			self.socket_is_using=True
			self.sendFrame(message)
			self.socket_is_using=False
			
		else:
			print('error occured: INVALID_STATE_ERR')
			
	#address-string
	#deliveryOption -object
	#replyHandler -function
	def registerHandler(self,address,deliveryOption=None,handler=None):
		
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
				timeInterval=0.01
			
			try:
				if self.Handlers[address] == None:
					self.Handlers[address]=[]
					message=json.dumps({'type':'register','address':address,'headers':headers,})
					self.socket_is_using=True
					self.sendFrame(message)
					self.socket_is_using=False
			except KeyError:
				self.Handlers[address]=[]
				
			time.sleep(timeInterval)	
			try:
				self.Handlers[address].append(handler)
			except Exception as e:
				print('Registration error:'+str(e))
		else:
			print('error occured: INVALID_STATE_ERR')
		
		
	#address-string
	#deliveryOption -object
	#replyHandler -function
	def unregisterHandler(self,address,deliveryOption=None):
		if self.isConnected() is True:
			message=None
			
			if deliveryOption!= None:
				headers=deliveryOption.headers
				replyAddress=deliveryOption.replyAddress
			else :
				headers=None
				replyAddress=None
			
			if self.Handlers[address]!= None:
				if len(self.Handlers) == 1:
					message=json.dumps({'type':'unregister','address':address,'headers':headers,})
					self.sendFrame(message)
				del self.Handlers[address]
			else:
				print('error occurred:unknown address')
				
		else:
			print('error occured: INVALID_STATE_ERR')
		