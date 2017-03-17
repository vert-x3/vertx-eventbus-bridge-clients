import Vertx.DeliveryOption as DeliveryOption
import unittest
import json


class TestDeliveryOption(unittest.TestCase):
    ('testing DeliveryOption')

    def test_addHeader(self):
        do = DeliveryOption.DeliveryOption()
        do.addHeader('type', 'text')
        header = {'type': 'text', }
        self.assertEqual(do.headers, header)
        do.addHeader('sessionID', '1234')
        headers = {'type': 'text', 'sessionID': '1234', }
        self.assertEqual(do.headers, headers)

    def test_deleteHeader(self):
        do = DeliveryOption.DeliveryOption()
        do.addHeader('type', 'text')
        do.addHeader('sessionID', '1234')
        do.deleteHeader('type')
        header = {'sessionID': '1234', }
        self.assertEqual(do.headers, header)

    def test_addReplyAddress(self):
        do = DeliveryOption.DeliveryOption()
        do.addReplyAddress('vert.x')
        self.assertEqual(do.replyAddress, 'vert.x')
        do.deleteReplyAddress()
        self.assertEqual(do.replyAddress, None)

if __name__ == '__main__':
    unittest.main()
