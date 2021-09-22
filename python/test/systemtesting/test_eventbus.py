import unittest
from . import EventBusBridgeStarter, CountDownLatch
from vertx import EventBus


class EventBusClientTests(unittest.TestCase):
    """
    This is the tests against a local test eventbus bridge
    """
    
    starter = None
    
    def __init__(self, *args, **kwargs):
        super(EventBusClientTests, self).__init__(*args, **kwargs)
    
    @classmethod
    def setUpClass(cls):
        cls.starter = EventBusBridgeStarter(debug=True)
        cls.starter.start()
        cls.starter.wait_started()
    
    @classmethod
    def tearDownClass(cls):
        cls.starter.stop()
    
    def test_register_unregister(self):
        latch = CountDownLatch(2)
        ebus = EventBus(options={'connect': True})
        
        def list_handlers(message):
            handler_list = message['body']['list']
            addresses = [e['address'] for e in handler_list]
            if "first" in message['headers'] and message['headers']['first']:
                self.assertIn("list-handler", addresses)
                self.assertIn("test-handler", addresses)
                ebus.unregister_handler("test-handler")
                self.assertIn("list-handler", ebus.handlers())
                self.assertNotIn("test-handler", ebus.handlers())
                ebus.send("list", reply_address="list-handler")
                latch.count_down()
            else:
                self.assertIn("list-handler", addresses)
                self.assertNotIn("test-handler", addresses)
                ebus.close()
                latch.count_down()
            # check list handler again, no test-handler anymore !!
        
        ebus.register_handler("list-handler", list_handlers)
        ebus.register_handler("test-handler", lambda x: print(x))
        self.assertIn("list-handler", ebus.handlers())
        self.assertIn("test-handler", ebus.handlers())
        ebus.send("list", reply_address="list-handler", headers={'first': True})
        latch.awaits(5)
    
    def test_send(self):
        latch = CountDownLatch()
        ebus = EventBus()
        ebus.connect()
        
        def handler(message):
            self.assertEqual(message['body']['hello'], 'world')
            ebus.close()
            latch.count_down()
        ebus.register_handler("echo-back", handler)
        ebus.send("echo", reply_address="echo-back", body={'hello': 'world'})
        latch.awaits(5)
    
    def test_send_with_handler(self):
        latch = CountDownLatch()
        ebus = EventBus()
        ebus.connect()
        
        def handler(message):
            self.assertEqual(message['body']['hello'], 'world')
            latch.count_down()
        ebus.send("echo", body={'hello': 'world'}, reply_handler=handler)
        latch.awaits(5)
        ebus.close()
    
    def test_publish(self):
        latch = CountDownLatch()
        ebus = EventBus()
        ebus.connect()
        
        def handler(message):
            print("got publish messages back")
            self.assertEqual(message['body']['hello'], 'world')
            self.assertEqual(message['headers']['name'], 'vertx-python')
            ebus.close()
            latch.count_down()
        ebus.register_handler("publish-back", handler)
        ebus.publish("publish-back", headers={'name': 'vertx-python'}, body={'hello': 'world'})
        latch.awaits(5)
    
    def test_send_auto_connect(self):
        latch = CountDownLatch()
        ebus = EventBus(options={'auto_connect': True})
        
        def handler(message):
            self.assertEqual(message['body']['hello'], 'world')
            ebus.close()
            latch.count_down()
        ebus.register_handler("echo-back", handler)
        ebus.send("echo", reply_address="echo-back", body={'hello': 'world'})
        latch.awaits(5)
    
    def test_send_no_connect(self):
        ebus = EventBus(options={'auto_connect': False})
        try:
            ebus.register_handler("echo-back", lambda x: print(x))
            self.fail("should not here")
        except Exception as e:
            self.assertEqual(str(e.args[0]), "socket has been closed.")
    
    def test_err_handler_bad_address(self):
        latch = CountDownLatch()
        
        def err_handler(message):
            self.assertEqual(message['type'], 'err')
            self.assertEqual(message['message'], 'access_denied')
            ebus.close()
            latch.count_down()
        ebus = EventBus(err_handler=err_handler)
        ebus.send('#-Bad_Address$', body={'name': 'python'})
        latch.awaits(5)
    
    def test_re_connect_with_registered_handlers(self):
        latch = CountDownLatch()
        
        def local_handler(message):
            self.assertEqual(message['type'], 'message')
            self.assertEqual(message['body']['name'], 'python')
            ebus.close()
            latch.count_down()
        ebus = EventBus()
        # register local before getting connect
        ebus._register_local("local-handler", local_handler)
        ebus.connect()
        ebus.send('local-handler', body={'name': 'python'})
        latch.awaits(5)


if __name__ == "__main__":
    unittest.main()
