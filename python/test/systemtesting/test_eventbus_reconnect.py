import time
import unittest
from . import EventBusBridgeStarter, CountDownLatch
from vertx import EventBus

"""
Tests client re connection, server may start after clients starting to connect.
"""


class EventBusClientReconnectTests(unittest.TestCase):

    # starter = None
    
    def __init__(self, *args, **kwargs):
        super(EventBusClientReconnectTests, self).__init__(*args, **kwargs)
    
    def test_reconnect_before_server_start(self):
        """
        Tests that server starts 5 seconds after client tries to connect to it.
        """
        latch = CountDownLatch()
        ebus = EventBus(options={"auto_connect": "True", "max_reconnect": "5"})
        starter = EventBusBridgeStarter(debug=False)
        # server will start after 5 seconds in background thread
        starter.start_async(delay=5)
        # this will block for 5 seconds to connect until server gets started
        ebus.connect()

        def handler(message):
            self.assertEqual(message['body']['hello'], 'world')
            ebus.close()
            print("Passed!")
            latch.count_down()
        ebus.register_handler("echo-back", handler)
        ebus.send("echo", reply_address="echo-back", body={'hello': 'world'})
        latch.awaits(10)
        starter.stop()

    def test_reconnect_server_restart(self):
        """
        Tests that server paused for 5 seconds, and restarts after that.
        All local handlers should be registered again after server restarted.
        """
        latch = CountDownLatch(2)
        ebus = EventBus(options={"auto_connect": "True", "max_reconnect": "5", "debug": "True"})
        starter = EventBusBridgeStarter(debug=True)

        def handler(message):
            self.assertEqual(message['body']['hello'], 'world')
            latch.count_down()

        try:
            starter.start()
            starter.wait_started()
            ebus.connect()
            ebus.register_handler("echo-back", handler)
            ebus.send("echo", reply_address="echo-back", body={'hello': 'world'})
            latch.awaits(5, to_count=1)
        finally:
            print("Now stop the server")
            starter.stop()

        print("Now wait for 5 seconds")
        time.sleep(5)
        try:
            print("Now start the server again.")
            starter.start()
            starter.wait_started()
            print("ebus should get connected automatically and the handler gets registered again.")
            ebus.send("echo", reply_address="echo-back", body={'hello': 'world'})
            latch.awaits(10)
        finally:
            print("close the client now...")
            ebus.close()
            starter.stop()


if __name__ == "__main__":
    unittest.main()
