# TCP-eventbus-client-C~~#~~

This is a TCP eventbus implementation for C# clients.

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

In order to use it in your project just link against the DLL `vertx-eventbus`. To build it use the `Docker` script. E.g.:

```sh
docker run --rm -v $(pwd):/workdir:Z -it microsoft/dotnet:latest /workdir/vertx-eventbus/build.sh
```

There is a working example under `examples/client.cs`.
