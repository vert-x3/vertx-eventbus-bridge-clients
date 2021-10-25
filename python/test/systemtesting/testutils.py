#!/usr/bin/python
# Authors:
# 2020: Wolfgang Fahl https://github.com/WolfgangFahl
# 2021: Lin Gao https://github.com/gaol
#
# This test starter borrows lots from https://github.com/rc-dukes/vertx-eventbus-python, thanks to Wolfgang Fahl
#

from datetime import datetime
from hashlib import md5
import os
import time
from subprocess import Popen, PIPE
import tempfile
from threading import Thread, Condition
import requests

STARTER_FAIL_INDICATOR = "Failed to start the TCP EventBus Bridge"
DEFAULT_WAIT_FOR = "Welcome to use EventBus Starter"
JAR_URL_TEMPLATE = "https://github.com/gaol/test-eventbus-bridge/releases/download/%s/test-ebridge-%s-fat.jar"


_FAT_JARS_ = {'1.0.0': '05cd3e187bf516db4685abb15c9bf983',
              '1.0.1': 'a5c837430e98357c5ee5e533cf22b5e9'}


class EventBusBridgeStarter:
    """
    An Vertx EventBus Bridge Starter, used to start a bridge server for integration testing
    """
    
    def __init__(self, jar_version='1.0.1', port=7000, wait_for=DEFAULT_WAIT_FOR, debug=False, conf=None):
        """
        construct me

        Args:
           jar_version(str: the bridge jar version to use
           port(int): the port to listen to, defaults to 7000
           wait_for(str): the output string on stderr of the java process to wait for
           debug(bool): True if debugging output should be shown else False - default: False
        """
        self.port = port
        self.wait_for = wait_for
        self.process = None
        self.started = False
        self.debug = debug
        self.conf = conf
        if jar_version not in _FAT_JARS_:
            print("%s is not a known version" % jar_version)
            exit(1)
        self.jar_version = jar_version
        temp_dir = tempfile.gettempdir().lower()
        self.jar_file = "%s/test-ebridge-%s-fat.jar" % (temp_dir, jar_version)
        self.jar_url = JAR_URL_TEMPLATE % (jar_version, jar_version)
        self.failed = False
    
    def start(self):
        try:
            if not os.path.exists(self.jar_file):
                if self.debug:
                    print("Downloading bridge jar from %s" % self.jar_url)
                req = requests.get(self.jar_url)
                with open(self.jar_file, 'wb') as f:
                    f.write(req.content)
                    f.close()
            with open(self.jar_file, 'rb') as f:
                jar_md5 = md5(f.read()).hexdigest()
            if _FAT_JARS_[self.jar_version] != jar_md5:
                print("%s is not a valid test eventbus bridge jar file" % self.jar_file)
                exit(1)
            if self.conf is None:
                self.process = Popen(['java', '-jar', self.jar_file], stderr=PIPE)
            elif type(self.conf) is dict or os.path.exists(self.conf):
                self.process = Popen(['java', '-jar', '-conf', self.conf, self.jar_file], stderr=PIPE)
            t = Thread(target=self._handle_output)
            t.daemon = True  # thread dies with the program
            t.start()
            # you need to wait for started with expected output
        except IOError as e:
            print(e)
            raise e
    
    def wait_started(self, time_out=30.0, time_step=0.1):
        """ wait for the java server to be started

        Args:
          time_out(float): the timeout in secs after which the wait fails with an Exception
          time_step(float): the time step in secs in which the state should be regularly checked

        :raise:
           :exception: wait timed out
           :exception: if bridge failed to start
        """
        time_left = time_out
        while not self.started and not self.failed and time_left > 0:
            time.sleep(time_step)
            time_left = time_left - time_step
        if time_left <= 0:
            raise Exception("wait for start time_out after %.3f secs" % time_out)
        if self.failed:
            raise Exception("failed to start vertx eventbus bridge")
        if self.debug:
            print("wait for start successful after %.3f secs" % (time_out - time_left))

    def _handle_output(self):
        """ handle the output of the java program"""
        out = self.process.stderr
        for bline in iter(out.readline, b''):
            line = bline.decode('utf8')
            if self.debug:
                print("Java: %s" % line)
            if self.wait_for in line:
                self.started = True
            if STARTER_FAIL_INDICATOR in line:
                self.failed = True
                self.stop()
                break
        out.close()
    
    def stop(self):
        if self.started:
            self.process.kill()
            self.process.wait()
            self.process = None
            self.started = False

    def start_async(self, delay=5):
        def _start_internal(_delay=delay):
            time.sleep(_delay)
            self.start()
            self.wait_started()
        start_thread = Thread(target=_start_internal, args=(delay,))
        start_thread.daemon = True
        start_thread.start()


class CountDownLatch:
    """
    CountDownLatch can be used for async testing
    """
    def __init__(self, count=1):
        self.count = count
        self.condition = Condition()
    
    def awaits(self, timeout=None, to_count=0):
        try:
            self.condition.acquire()
            start = datetime.now()
            while self.count > to_count:
                self.condition.wait(timeout / 10)  # divides each step by 10
                if timeout is not None:
                    spent = (datetime.now() - start).seconds
                    if spent > timeout:
                        raise Exception("timeout after waiting for %d seconds!" % spent)
        finally:
            self.condition.release()
    
    def count_down(self):
        try:
            self.condition.acquire()
            self.count -= 1
            self.condition.notifyAll()
        finally:
            self.condition.release()
    
    def get_count(self):
        return self.count
