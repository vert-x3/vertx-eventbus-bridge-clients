import unittest
from . import EventBusBridgeStarter, CountDownLatch
from vertx import EventBus


class EventBusClientTLSTests(unittest.TestCase):
    """
    Tests against EventBus Bridge with TLS enabled.
    """
    def __init__(self, *args, **kwargs):
        super(EventBusClientTLSTests, self).__init__(*args, **kwargs)

    def test_tls_client_auth_off(self):
        """
        Tests when tls.CLIENT_AUTH is disabled.
        """
        latch = CountDownLatch()
        starter = EventBusBridgeStarter(debug=True, conf={"server-options":
                                                               {"ssl": "true", "clientAuth": "NONE", "pemKeyCertOptions":
                                                                   {"keyPath": "test/systemtesting/ca.key",
                                                                    "certPath": "test/systemtesting/ca.crt"}
                                                                }
                                                           })
        try:
            starter.start()
            starter.wait_started()

            ebus = EventBus(options={"debug": "True", "ssl_options": {"ca_file": "test/systemtesting/ca.crt"}})
            ebus.connect()

            def handler(message):
                self.assertEqual(message['body']['hello'], 'world')
                ebus.close()
                print("Passed!")
                latch.count_down()

            ebus.register_handler("echo-back", handler)
            ebus.send("echo", reply_address="echo-back", body={'hello': 'world'})
            latch.awaits(10)
        finally:
            starter.stop()

    def test_tls_client_auth_on(self):
        """
        Tests when tls.CLIENT_AUTH is enabled.
        """
        latch = CountDownLatch()
        starter = EventBusBridgeStarter(debug=True, conf={"server-options":
                                                             {"ssl": "true", "clientAuth": "REQUIRED",
                                                              "pemKeyCertOptions":
                                                                 {"keyPath": "test/systemtesting/ca.key",
                                                                  "certPath": "test/systemtesting/ca.crt"},
                                                              "pemTrustOptions": {"certPaths": ["test/systemtesting/ca.crt"]}
                                                             }
                                                          })
        try:
            starter.start()
            starter.wait_started()

            ebus = EventBus(options={"debug": "True", "ssl_options": {"ca_file": "test/systemtesting/ca.crt",
                                                                      "cert_file": "test/systemtesting/ca.crt",
                                                                      "key_file": "test/systemtesting/ca.key"}})
            ebus.connect()

            def handler(message):
                self.assertEqual(message['body']['hello'], 'world')
                ebus.close()
                print("Passed!")
                latch.count_down()

            ebus.register_handler("echo-back", handler)
            ebus.send("echo", reply_address="echo-back", body={'hello': 'world'})
            latch.awaits(10)
        finally:
            starter.stop()


if __name__ == "__main__":
    unittest.main()