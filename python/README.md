# TCP-eventbus-client-Python

This is a TCP eventbus implementation for python clients.

example:

```python
class Client():

#Handler
  def Handler(self,message):
    if message != None:
      print(message['body']['result'],'4');

eb=Vertx.Eventbus(Client(),'localhost', 7000)

#jsonObject -body
body={'msg':'add 4 to 0',}

#DeliveryOption
do=Vertx.DeliveryOption();
do.addHeader('type','text')
do.addHeader('size','small')
do.addReplyAddress('add')
do.setTimeInterval(5)

#register handler
eb.registerHandler('add',Client.Handler);

#send
eb.send('add',body,do)

#close after 5 seconds
eb.closeConnection(5)
```

## Install locally

```
# build the package
python setup.py sdist

# With the tar file you can install it anywhere
pip install --user path_to_tarfile.tar.gz
```
