using io.vertx;
using System;
using Newtonsoft.Json.Linq;

public class client
{
    public static int i=0;
    public static void Main(string[] args){
     try
        {
            io.vertx.Eventbus eb = new io.vertx.Eventbus();
            
            Console.WriteLine("i:"+client.i);

            Headers h = new Headers();
            h.addHeaders("type", "maths");
            JObject body_add =new JObject();
            body_add.Add("message","add");

            //sending with time out = 5 secs
            eb.send(
                "pcs.status",//address
                body_add,//body
                "pcs.status",//reply address
                h, //headers
                (new ReplyHandlers("pcs.status",//replyhandler address
                   new Action<bool, JObject>( //replyhandler function
                       (err, message) =>
                       {
                        if (err == false)
                            client.i += 5;
                        Console.WriteLine(message);
                       }
                   )
                )
               ),
               5);
            
            Console.WriteLine("i:"+client.i);

            JObject body_sub =new JObject();
            body_sub.Add("message","sub");

            //sending with default time out 
            eb.send(
                "pcs.status",//address
                body_sub,//body
                "pcs.status",//reply address
                h, //headers
                (new ReplyHandlers("pcs.status",//replyhandler address
                   new Action<bool, JObject>( //replyhandler function
                       (err, message) =>
                       {
                        if (err == false)
                            client.i -= 5;
                        Console.WriteLine(message);
                       }
                   )
                )
               ));
           
            eb.register(
                "pcs.status",
                (new Handlers(
                    "pcs.status",
                    new Action<JObject>(
                        message =>
                        {
                        client.i += 5;
                        Console.WriteLine(message);
                        }
                    )
                )
            ));

            Console.WriteLine("i:"+client.i);

            //send a message without a replyhandler
            eb.send("pcs.status", body_add, "pcs.status", h);
            //publish
            JObject body_close =new JObject();
            body_close.Add("message","close");
            eb.publish("pcs.status", body_close, h);

            //close the socket
            eb.CloseConnection(5);
            Console.WriteLine("i:"+client.i);
        }
        catch (Exception e)
        {
            System.Console.WriteLine(e);
        }

 }
}
