# TCP-eventbus-client-C~~#~~

This is a TCP eventbus implementation for C# clients. The protocol is quite simple:

Get from Nuget at 

https://www.nuget.org/packages/VertxEventbus/1.2.0-beta

https://www.nuget.org/packages/vertx-eventbus/2.2.0-pre

* 4bytes int32 message length (big endian encoding)
* json string
* built-in keys
        
        1) type: (String, required) One of "send", "publish", "register", "unregister".
        
        2) headers: (Object, optional) Headers with JSON format.
        
        3) body: (Object, optional) Message content in JSON format.
        
        4) address: (String, required) Destination address
        
        5) replyAddress: (String, optional) Address for replying to.
        

example:

```cs
        public class client
        {
            public static void Main(string[] args){
             
             io.vertx.Eventbus eb=new io.vertx.Eventbus();
            
             Headers h=new Headers();
             h.addHeaders("type","maths");
             
             //body
             JObject body=new JObject();
             body.Add("message","add");
             
             //sending with time out = 5 secs
             eb.send(
                 "pcs.status",//address
                 body,//body
                 "pcs.status",//reply address
                 h, //headers
                 (new ReplyHandlers("pcs.status",//replyhandler address
                    new Action<bool,string>( //replyhandler function
                        (err,message)=>{
                            Console.WriteLine("replyhandler:"+message);
                        }
                    )
                 )
                ),
                5);//timeout
          }
        }
```

