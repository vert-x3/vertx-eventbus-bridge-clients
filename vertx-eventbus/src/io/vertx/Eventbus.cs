/*The MIT License (MIT)

Copyright (c) 2016 Jayamine Alupotha

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.*/

//@author: Jayamine Alupotha

using System;
using System.IO;
using System.Collections.Generic;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using Newtonsoft.Json.Linq;

namespace io.vertx
{
    //This is the structure for JSON message
     struct JsonMessage{
        String type;
        String address;
        String replyAddress;
        JObject body;
        JObject headers;
        //create
        public void create(string new_type,string new_address,string new_replyAddress,JObject new_body){
            if(new_type==null) {
                throw new System.ArgumentException("JsonMessage:type cannot be null");
            }
            if(new_address==null) {
                throw new System.ArgumentException("JsonMessage:address cannot be null");
            }
            type=new_type;
            address=new_address;
            replyAddress=new_replyAddress;
            body=new_body;
        }
        //to string
        public String getMessage(){
            if(replyAddress==null) replyAddress="null";
            JObject jsonMessage=new JObject();
            jsonMessage.Add("type",type);
            jsonMessage.Add("address",address);
            jsonMessage.Add("replyAddress",replyAddress);
            jsonMessage.Add("body",body);
            jsonMessage.Add("headers",headers);
            return jsonMessage.ToString();
        }
        
        public void setHeaders(Headers h){
            headers=h.getHeaders();
        }
    }

    //This is the structure for headers
    public struct Headers{
        JObject headers;

        public void addHeaders(String headerName,String header){
            headers=new JObject();
            headers.Add(headerName,header);
        }
        //delete headers
        public void deleteHeaders(){
            headers=null;
        }

        public JObject getHeaders(){
            return headers;
        }
    }

    //This is the structure for replyHandlers
    public struct ReplyHandlers{
        Action<bool,JObject> function;
        public string address;
        public ReplyHandlers(string address,Action<bool,JObject> func){
            this.function=func;
            this.address=address;
        }

        public void handle(bool error,JObject message){
            this.function(error,message);
        }

        public bool isNull(){
            if(this.function==null && this.address==null) return true;
            return false; 
        }
        public void setNull(){
            this.function=null ;
            this.address=null; 
        }
    }

    //This is the structure for Handlers
    public struct Handlers{
        string address;
        Action<JObject> function;
        public Handlers(string address,Action<JObject> func){
            this.address=address;
            this.function=func;
        }

        public void handle(JObject message){
            function(message);
        }
    }

/*Eventbus constructor
#	input parameters
#		1) host	- String
#		2) port	- integer(>2^10-1)
#		3) TimeOut - int- receive TimeOut
#	inside parameters
#		1) socket
#		2) handlers - List<address,Handlers> 
#		3) state -integer
#		4) ReplyHandler - <address,function>
#       5) fileLock - object
#Eventbus state
#	0 - not connected/failed
#	1 - connecting
#	2 - connected /open
#	3 - closing
#	4 - closed*/
    public class Eventbus
    {
        Socket sock = new Socket(AddressFamily.InterNetwork,SocketType.Stream, ProtocolType.Tcp);
	    Dictionary<String,List<Handlers>> Handlers=new Dictionary<String,List<Handlers>>();
	    int state = 0;
	    ReplyHandlers replyHandler;
        int TimeOut;
        Thread t ;
        object Lock=new Object();
        bool clearReplyHandler=false;
        static object fileLock=new Object();

        //constructor
        public Eventbus(String host="127.0.0.1",int port=7000,int TimeOut=1000){

            if(TimeOut<500) this.TimeOut=500;
            else this.TimeOut=TimeOut;

            //connect
            try{
                this.state = 1;
                System.Net.IPAddress ipaddress = System.Net.IPAddress.Parse(host);
                IPEndPoint remoteEP = new IPEndPoint(ipaddress, port);
                sock.Connect(remoteEP);
                this.t = new Thread(new ThreadStart(this.receive));
                t.Start();
                this.state = 2;
                }
            catch (Exception e){
                this.state=4;
                PrintError(1,"Could not establish the connection\n"+e);
                throw new System.Exception("Not connected "+host+" "+port+"\n",e);
            }
        }
//Connection send and receive--------------------------------------------------------------------

        public bool isConnected(){
            if(this.state==2) return true;
            return false;
        }

        bool sendFrame(JsonMessage jsonMessage){
            try{
                String message_s=jsonMessage.getMessage();
                UTF8Encoding utf8 =new UTF8Encoding();
                byte[] headerBuffer = new byte[4];
                byte[] bodyBuffer = utf8.GetBytes(message_s);
                var bodyLength = bodyBuffer.Length;
                headerBuffer = BitConverter.GetBytes((UInt32) bodyLength);
                if (BitConverter.IsLittleEndian){
                    Array.Reverse(headerBuffer);
                }

                // The message might not be sent all at once, but get split up into chunks.
                // If we don't want to sent an incomplete message, we have to loop over the send requests. 
                int bytesSentHeader = 0;
                while(bytesSentHeader < 4)
                {
                    bytesSentHeader += sock.Send(headerBuffer, bytesSentHeader, 4 - bytesSentHeader, SocketFlags.None);
                }
                
                // The message might not be sent all at once, but get split up into chunks.
                // This happens often if the message is large.
                // If we don't want to sent an incomplete message, we have to loop over the send requests. 
                int bytesSentBody = 0;
                while(bytesSentBody < bodyLength)
                {
                    bytesSentBody += sock.Send(bodyBuffer, bytesSentBody, bodyLength - bytesSentBody, SocketFlags.None);
                }
                return true;
            }catch(Exception e){
                PrintError(2,"Can not send the message\n"+e);
                throw new System.Exception("Can not send the message\n",e);
            }
        }

        void receive(){
            
            while (true)
            {
                try{
                    if(this.sock.Poll(this.TimeOut,SelectMode.SelectRead)){
                        //check state
                    if(this.state==2){  
                            UTF8Encoding utf8 =new UTF8Encoding();
                            byte[] lengthBuffer = new byte[4];
                            
                            int receivedBytesLengthBuffer = 0;
                            // If the message is large, it might be sent in several chunks.
                            // We have to collect all the chunks, otherwise we get an incomplete message.
                            // Normally packages are bigger than 4 bytes, so this is just to be sure. 
                            while(receivedBytesLengthBuffer < 4){
                                receivedBytesLengthBuffer += sock.Receive(lengthBuffer, receivedBytesLengthBuffer, 4 - receivedBytesLengthBuffer, SocketFlags.None);
                            }

                            if (BitConverter.IsLittleEndian){
                                Array.Reverse(lengthBuffer);
                            }
                            
                            int messageSize = BitConverter.ToInt32(lengthBuffer, 0);
                            
                            byte[] receiveBuffer = new byte[messageSize];

                            int receivedBytesRecvBuff = 0;
                            // If the message is large, it might be sent in several chunks.
                            // We have to collect all the chunks, otherwise we get an incomplete message which results in a JSON parsing exception.
                            // This is NOT so uncommon, if the serialized JSON-Objects are large.
                            while(receivedBytesRecvBuff < messageSize) {
                                receivedBytesRecvBuff += sock.Receive(receiveBuffer, receivedBytesRecvBuff, messageSize - receivedBytesRecvBuff, SocketFlags.None);
                            }

                            String message_string = utf8.GetString(receiveBuffer, 0, receivedBytesRecvBuff);
                            JObject message=JObject.Parse(message_string);
                            if(message.GetValue("type").ToString()=="message"){
                                string address=message.GetValue("address").ToString();

                                if(address== null){
                                    PrintError(3,"Failed Message\n"+message);
                                }else{
                                    //Handlers
                                    lock (Lock)
                                    {
                                        //handlers 
                                        if(Handlers.ContainsKey(address)==true){
                                            foreach (Handlers handler in Handlers[address])
                                            {
                                                handler.handle(message);
                                            }
                                            //reply address
                                            if(this.replyHandler.isNull()==false){
                                                if(this.replyHandler.address.Equals(address)==true){
                                                    this.replyHandler.handle(false,message);
                                                    this.replyHandler.setNull();
                                                    clearReplyHandler=true;
                                                }
                                            }
                
                                        }
                                        //reply handler
                                        else if(this.replyHandler.isNull()==false){
                                            if(this.replyHandler.address.Equals(address)==true){
                                                this.replyHandler.handle(false,message);
                                                this.replyHandler.setNull();
                                                clearReplyHandler=true;
                                            }
                                            else{
                                                PrintError(3,"No handlers to handle this message\n"+message);
                                            }
                                        }else{
                                            PrintError(3,"No handlers to handle this message\n"+message);
                                        }
                                    }
                                }
                            }
                            if(message.GetValue("type").ToString()=="err"){
                                if(this.replyHandler.isNull()==false){
                                    this.replyHandler.handle(true,message);
                                    this.replyHandler.setNull();
                                    clearReplyHandler=true;
                                }
                                else{
                                    PrintError(3,"No handlers to handle this message\n"+message);
                                }
                                
                            }
                    }
                    else{
                        return;
                    }
                        
                    }
                    else if(sock.Poll(100,SelectMode.SelectError)){
                        PrintError(4,"Error at socket polling");
                    }
                }
                catch(Exception e){
                    PrintError(5,e.ToString());
                }
            }
        }

      public void CloseConnection(int timeInterval){
           if(this.state==1)
            this.sock.Shutdown(SocketShutdown.Both);
           else{
               try{
                    Thread.Sleep(timeInterval*1000);
                    this.state=3;
                    this.sock.Shutdown(SocketShutdown.Both);
                    this.state=4;
               }catch(Exception e){
                    PrintError(6,e.ToString());
               }
           }
       }
//send, receive, register, unregister ------------------------------------------------------------

        /*
        #address-string
        #body - json object
        #headers- struct Headers
        #replyAddress - string
        */
        public void send(string address,JObject body,string replyAddress,Headers headers){
            JsonMessage message=new JsonMessage();
            message.create("send",address,replyAddress,body);
            message.setHeaders(headers);

            while(true){
                if(this.sock.Poll(this.TimeOut,SelectMode.SelectWrite)){
                    try{this.sendFrame(message);}
                    catch(Exception e){
                        PrintError(7,e.ToString());
                        throw new System.Exception("",e);
                    }
                    break;
                }
            }   
            
        }
        /*
        #address-string
        #body - json object
        #headers- struct Headers
        #replyAddress - string
        #replyHandler - ReplyHandler
        #timeInterval - int -sec
        */
        public void send(string address,JObject body,string replyAddress,Headers headers,ReplyHandlers replyHandler,int timeInterval=10){
            JsonMessage message=new JsonMessage();
            message.create("send",address,replyAddress,body);
            message.setHeaders(headers);
            this.replyHandler=replyHandler;
            while(true){
                if(this.sock.Poll(this.TimeOut,SelectMode.SelectWrite)){
                    try{this.sendFrame(message);}
                    catch(Exception e){
                        PrintError(7,e.ToString());
                        throw new System.Exception("",e);
                    }
                    break;
                }
            }
            while(timeInterval>0){
                Thread.Sleep(1000);
                lock (Lock)
                {
                    if(clearReplyHandler==true){
                    break;
                    }
                    timeInterval--;      
                }
            }
            if(timeInterval==0){
                JObject err=new JObject();
                err.Add("message","TIMEOUT ERROR");
                replyHandler.handle(true,(new JObject()));

            }

            
        }

        /*
        #address-string
        #body - json object
        #headers
        */
        public void publish(string address,JObject body,Headers headers){
            JsonMessage message=new JsonMessage();
            message.create("publish",address,null,body);
            message.setHeaders(headers);

            while(true){
                if(this.sock.Poll(this.TimeOut,SelectMode.SelectWrite)){
                    try{this.sendFrame(message);}
                    catch(Exception e){
                        PrintError(8,e.ToString());
                        throw new System.Exception("",e);
                    }
                    break;
                }
            } 
        } 


        /*
        #address-string
        #headers
        #handler -Handlers
        */
        public void register(string address,Handlers handler){

            if(Handlers.ContainsKey(address)==false){
                JsonMessage message=new JsonMessage();
                message.create("register",address,null,null);

                while(true){
                    if(this.sock.Poll(this.TimeOut,SelectMode.SelectWrite)){
                        try{this.sendFrame(message);}
                        catch(Exception e){
                            PrintError(9,e.ToString());
                            throw new System.Exception("",e);
                        }
                        break;
                    }
                }
                List<Handlers> list=new List<Handlers>();
                list.Add(handler);
                Handlers.Add(address,list);
            }
            else{
                List<Handlers> handlers=Handlers[address];
                handlers.Add(handler);
                Handlers.Add(address,handlers);
            }
            

        }

        /*
        #address-string
        #headers - Headers
        */
        public void unregister(string address){
            if(Handlers.ContainsKey(address)==false){
                JsonMessage message=new JsonMessage();
                message.create("unregister",address,null,null);

                while(true){
                    if(this.sock.Poll(this.TimeOut,SelectMode.SelectWrite)){
                        try{this.sendFrame(message);}
                        catch(Exception e){
                            PrintError(10,e.ToString());
                            throw new System.Exception("",e);
                        }
                        break;
                    }
                }
            }
            else{
                Handlers.Remove(address);
            }
        }
//Errors ------------------------------------------------------------------------------------------

       public static void PrintError(int code,String error){
            lock(fileLock){
                var name="error_log_.txt";
                try{
                    using (FileStream aFile = new FileStream(name, FileMode.Append, FileAccess.Write))
                        using (StreamWriter log = new StreamWriter(aFile)) {
                            log.WriteLine("********** "+DateTime.Now+" **********\n");
                            log.WriteLine("CODE: "+code+"\n");
                            log.WriteLine(error+"\n\n");   
                        }
        
                }catch(Exception e){
                    Console.WriteLine("Could not write to the log file\n"+e.ToString());

                }
            }
        }    
    }
}
