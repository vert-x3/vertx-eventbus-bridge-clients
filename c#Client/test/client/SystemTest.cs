using eventbus;
using System;
using Xunit;

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

            //sending with time out = 5 secs
            eb.send(
                "pcs.status",//address
                "{\"message\":\"add\"}",//body
                "pcs.status",//reply address
                h, //headers
                (new ReplyHandlers("pcs.status",//replyhandler address
                   new Action<bool, string>( //replyhandler function
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


            //sending with default time out 
            eb.send(
                "pcs.status",//address
                "{\"message\":\"sub\"}",//body
                "pcs.status",//reply address
                h, //headers
                (new ReplyHandlers("pcs.status",//replyhandler address
                   new Action<bool, string>( //replyhandler function
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
                    new Action<string>(
                        message =>
                        {
                    SystemTest.i += 5;
                        }
                    )
                )
            ));

            //send a message without a replyhandler
            eb.send("pcs.status", "{\"message\":\"add\"}", "pcs.status", h);

            //publish
            eb.publish("pcs.status", "{\"message\":\"going to close\"}", h);

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
