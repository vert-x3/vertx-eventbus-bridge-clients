# TCP-eventbus-client-Python

This is a TCP eventbus implementation for python clients.

example:

```python
from vertx import EventBus

ebus = EventBus('localhost', 7000)

# Define a handler to handle message
def handler(msg):
    print("Got message from server: %s" % msg)

# Connects to target bridge, it will connect automatically
ebus.connect()

# register a handler on address `test`
ebus.register_handler("test", handler)

# send a json message to address: `test`, the handler will be called
ebus.send("test", body={'name': 'python'})

# You can also send a message, and specify the reply_address
ebus.send("echo", reply_address="test", body={'desc': 'send to echo, reply to test'})

# publish message
ebus.publish("publish-address", body={'name': 'python'})

# unregister the handler after everything is done
ebus.unregister_handler("test")

# close the EventBus to release resources, after closed, it cannot be used anymore
ebus.close()

```

## Build and install locally

```
# build the package
./build.sh --dist

# With the tar file you can install it anywhere
pip install dist/vertx-eventbus-client*.tar.gz

# Install from PyPI:
pip install vertx-eventbus-client
```

