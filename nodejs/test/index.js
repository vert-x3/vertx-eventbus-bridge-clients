var assert = require('assert');
var EventBus = require('../lib/tcp-vertx-eventbus');

describe('echo test', function () {
  it('should echo the same message that was sent', function (done) {
    var eb = new EventBus('localhost', 7000);


    eb.onerror = function (err) {
      console.error(err);
      assert.fail();
    };

    eb.onopen = function () {
      // send a echo message
      eb.send('echo', {value: 'vert.x'}, function (err, res) {
        if (err) {
          assert.fail();
          return;
        }

        assert.equal(res.body.value, 'vert.x');
        done();
      });
    };
  });
});
