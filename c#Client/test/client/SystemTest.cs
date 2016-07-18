using eventbus;
using System;
using Xunit;
using Newtonsoft.Json.Linq;

public class SystemTest
{
    public static int i = 0;

    [Fact]
    public void testing()
    {
        try
        {
            eventbus.Eventbus eb = new eventbus.Eventbus();

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



            eb.register(
                "pcs.status",
                h,
                (new Handlers(
                    "pcs.status",
                    new Action<JObject>(
                        message =>
                        {
                    SystemTest.i += 5;
                        }
                    )
                )
            ));

            //send a message without a replyhandler
            eb.send("pcs.status", body_add, "pcs.status", h);

            //publish
            JObject body_close =new JObject();
            body_close.Add("message","close");
            eb.publish("pcs.status", body_close, h);

            //close the socket
            eb.CloseConnection(5);
            Assert.Equal(10, i);
        }
        catch (Exception e)
        {
            System.Console.WriteLine(e);
        }

    }
}
