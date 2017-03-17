import Vertx.Eventbus as Eventbus
import Vertx.DeliveryOption as DeliveryOption
import json
import unittest
import time

#replyHandler (self,error,message)
#handlers (self,message)


class Client(unittest.TestCase):
    ('System Testing')
    result = {'msg': 'test', }

    # Handler
    def Handler(self, message):
        if message != None:
            Client.result = message['body']

    # replyHandler
    def replyHandler(self, error, message):
        pass

    def test_send(self):
        c = Client()
        eb = Eventbus.Eventbus(c, 'localhost', 7001)

        #jsonObject -body
        body = {'msg': 'test1', }

        # DeliveryOption
        do = DeliveryOption.DeliveryOption()
        do.addHeader('type', 'text')
        do.addHeader('size', 'small')
        do.addReplyAddress('echo')
        do.setTimeInterval(5)

        # register handler
        eb.registerHandler('echo', Client.Handler)
        # send
        eb.send('echo', body, do)

        # without deliveryoption
        eb.send('echo', body, Client.replyHandler)
        assert(Client.result == {'msg': 'test1', })

        # without replyHandler
        body = {'msg': 'test2', }

        eb.send('echo', body, do)

        # close after 1 seconds
        eb.closeConnection(1)
        assert(Client.result == {'msg': 'test2', })

if __name__ == '__main__':
    unittest.main()
