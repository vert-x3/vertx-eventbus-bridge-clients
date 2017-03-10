using io.vertx;
using System;
using Xunit;
using Newtonsoft.Json.Linq;
using System.IO;

//@author: Jayamine Alupotha
public class SystemTest
{
    public static int i = 0;

    [Fact]
    public void test_system_testing()
    {
        try
        {
            Eventbus eb = new Eventbus("127.0.0.1",7000);

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
                               SystemTest.i += 5;
                       }
                   )
                )
               ),
               5);
            Assert.Equal(5, i);

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
                               SystemTest.i -= 5;
                       }
                   )
                )
               ));
            Assert.Equal(0, i);

            Handlers hand=new Handlers(
                    "pcs.status",
                    new Action<JObject>(
                        message =>
                        {
                    SystemTest.i += 5;
                        }
                    )
                );

            eb.register("pcs.status",hand);

            //send a message without a replyhandler
            eb.send("pcs.status", body_add, "pcs.status", h);

            //waiting for the message
            System.Threading.Thread.Sleep(1000);

            //unregister
            eb.unregister("pcs.status");

             //publish
            JObject body_close =new JObject();
            body_close.Add("message","close");
            eb.publish("pcs.status", body_close, h);

            //close the socket
            eb.CloseConnection(5);
            Assert.Equal(5, i);
        }
        catch (Exception e)
        {
            System.Console.WriteLine(e);
        }

    }

    [Fact]
    public void test_2_connectionError()
    {
        bool errorFound=false;
        try
        {
            io.vertx.Eventbus eb = new io.vertx.Eventbus("127.0.0.1",7001);
            //close the socket
            eb.CloseConnection(5);
        }catch(Exception){
            errorFound=true;
        }
        Assert.Equal(true,errorFound);
        bool fileFound=false;

            try{
                string path="error_log_.txt";
                if (File.Exists(path))
                {
                    fileFound=true;
                }
            }catch(Exception e){
                System.Console.WriteLine(e);
            }
            Assert.Equal(true,fileFound);
    }

    [Fact]
    public void test_3_noHandlers()
    {
        try
        {
            io.vertx.Eventbus eb = new io.vertx.Eventbus();

            Headers h = new Headers();
            h.addHeaders("type", "maths");
            JObject body_add =new JObject();
            body_add.Add("message","add");

            //sending with time out = 5 secs
            eb.send(
                "pcs.status",//address
                body_add,//body
                "pcs.status.c",//reply address-no handlers
                h, //headers
                (new ReplyHandlers("pcs.status",//replyhandler address
                   new Action<bool, JObject>( //replyhandler function
                       (err, message) =>
                       {
                    if (err == false)
                               SystemTest.i += 5;
                       }
                   )
                )
               ),
               5);
            //close the socket
            eb.CloseConnection(5);
            bool fileFound=false;

            try{
                string path="error_log_.txt";
                if (File.Exists(path))
                {
                    fileFound=true;
                }
            }catch(Exception e){
                System.Console.WriteLine(e);
            }
            Assert.Equal(true,fileFound);
            Assert.Equal(5, i);
        }catch(Exception e){
             System.Console.WriteLine(e);
        }
    }

}
