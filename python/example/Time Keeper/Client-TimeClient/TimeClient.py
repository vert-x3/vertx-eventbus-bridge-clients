import Vertx.Eventbus as Eventbus
import Vertx.DeliveryOption as DeliveryOption
import json
import time

#replyHanlder (self,error,message)
#handlers (self,message)


class TimeClient:

    # replyHandler for "Get.Time"
    def printTime(self, error, message):
        if error != None:
            print(error)
        if message != None:
            print(message['body'])

    # replyHandler for "Get.Date"
    def printDate(self, error, message):
        if error != None:
            print(error)
        if message != None:
            print(message['body'])

    # Handler for errors
    def Handler(self, message):
        if message != None:
            print('Update:')
            print(message)


eb = Eventbus.Eventbus(TimeClient(), 'localhost', 7000)

#jsonObject -body
body_date = {'message': 'send date', }

# DeliveryOption
do = DeliveryOption.DeliveryOption()
do.addHeader('type', 'text')
do.addHeader('size', 'small')
do.addReplyAddress('Date')
do.setTimeInterval(10)

# send - get data
eb.send('Date', body_date, do, TimeClient.printDate)

#jsonObject -body
body_time = {'message': 'send time', }

# change reply address
do.addReplyAddress('Time')

# send- get time
eb.send('Time', body_time, do, TimeClient.printTime)

# change reply address
do.addReplyAddress('Get')

# register handler
eb.registerHandler('Get', TimeClient.Handler)

#jsonObject -body
body_get = {'message': 'Thanks time keeper', }

# send- get time
eb.send('Time', body_time, do, TimeClient.printTime)
# send - get data
eb.send('Date', body_date, do, TimeClient.printDate)

# unregister handler
eb.unregisterHandler('Get')

#publish- get
eb.publish('Get', body_get, do)

# close after 5 seconds
eb.closeConnection(5)
