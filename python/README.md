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

### Options

The options supported when creating the EventBus are:
* `timeout` - the socket timeout, in seconds, defaults to `60` seconds.
* `connection_timeout` - connection timeout, in seconds, defaults to `600` seconds.
* `retry_interval` - retry interval on connection, in seconds, defaults to `5` seconds.
* `ping_interval` - heart beat for ping/pong, in seconds, defaults to `5` seconds.
* `reply_timeout` - message reply timeout, in seconds, defaults to `60` seconds.
* `auto_connect` - if client should connect to server automatically if it is not connected yet, or on failures, defaults to `True`.
* `ssl_options` - the `ssl` related options, please refer to [SSL Options](#ssl-options) below.

## TLS support

This client supports TLS connection, you need to either specify the `ssl_options` or a `ssl.SSLContext`:

```python
from vertx import EventBus

# for no ClientAuth required, just specify the ca_file:
ebus = EventBus(options={"ssl_options": {"ca_file": "/path/to/ca.crt"}})

# you can also specify the ca_path(directory of the certs) or ca_data:
ebus = EventBus(options={"ssl_options": {"ca_path": "/path/to/certs/"}})
ebus = EventBus(options={"ssl_options": {"ca_data": "my_cert_data"}})

# for ClientAuth, specify cert_file and key_file:
ebus = EventBus(options={"ssl_options": {"ca_file": "/path/to/ca.crt", "cert_file": "/path/to/client.crt"},
                         "key_file": "/path/to/client.key"})

# You can also specify a SSLContext directly if you have one already:
my_content = create_ssl_context()
ebus = EventBus(host="localhost", port=7000, ssl_context=my_context)

```

### SSL Options

The SSL options supported by this client are:
* `ca_file` - The CA certificate file which is used to verity the server.
* `ca_path` - The directory where the CA certificates are.
* `ca_data` - The CA certificate data which is used to verify the server.
* `ciphers` - The ciphers client wants to use on ssl handshake.
* `check_hostname` - Should check hostname or not.
  When any of `ca_*` is specified, it is `True` if it is not specified.
* `verify_mode` - Verify mode when doing handshake.
  When any of `ca_*` is specified, it is `ssl.CERT_REQUIRED` if it is not specified.
* `cert_file` - The client certificate file in ClientAuth case.
* `key_file` - The client key file in ClientAuth case.
* `key_password` - The password of client key file in ClientAuth case.


## Build and install locally

```
# build the package
./build.sh --dist

# With the tar file you can install it anywhere
pip install dist/vertx-eventbus-client*.tar.gz

# Install from PyPI:
pip install vertx-eventbus-client
```

## Build and Deploy to PyPi:

* Update the version in the `setup.py`:
```shell
# change this value to the desired version, like: 1.0.0
version="1.0.0.dev0"
```

* Use the following command to build and publish to PyPi:
```shell
./build.sh --publish
```
then you will be asked for username and password like:
```shell
Publish the package to Pypi.org
Uploading distributions to https://upload.pypi.org/legacy/
Enter your username:
```

> NOTE: You need to have `twine` installed in your machine to be able to publish.
