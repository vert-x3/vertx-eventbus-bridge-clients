using eventbus;
using System;


public class client
{
    public static int i=0;
    public static void Main(string[] args){
     try{
     eventbus.Eventbus eb=new eventbus.Eventbus();
    
     Headers h=new Headers();
     h.addHeaders("type","maths");

     //sending with time out = 5 secs
     eb.send(
         "pcs.status",//address
         "{\"message\":\"add\"}",//body
         "pcs.status",//reply address
         h, //headers
         (new ReplyHandlers("pcs.status",//replyhandler address
            new Action<bool,string>( //replyhandler function
                (err,message)=>{
                    Console.WriteLine("replyhandler:"+message);
                    if(err==false)
                        client.i+=5;
                }
            )
         )
        ),
        5);
        Console.WriteLine("i :"+i);

     //sending with default time out 
     eb.send(
         "pcs.status",//address
         "{\"message\":\"sub\"}",//body
         "pcs.status",//reply address
         h, //headers
         (new ReplyHandlers("pcs.status",//replyhandler address
            new Action<bool,string>( //replyhandler function
                (err,message)=>{
                    Console.WriteLine("replyhandler:"+message);
                    if(err==false)
                        client.i-=5;
                }
            )
         )
        ));

        Console.WriteLine("i :"+i);

     
    eb.register(
        "pcs.status",
        h,
        (new Handlers(
            "pcs.status",
            new Action<string>(
                message=>{
                    Console.WriteLine(message);
                    client.i+=5;
                }
            )
        )
    ));

     //send a message without a replyhandler
     eb.send("pcs.status","{\"message\":\"add\"}","pcs.status",h);
    
     //publish
     eb.publish("pcs.status","{\"message\":\"going to close\"}",h);

     //close the socket
     eb.CloseConnection(5);
     
     Console.WriteLine("i :"+i);
    }catch(Exception e){
         System.Console.WriteLine(e);
    }

 }
}
