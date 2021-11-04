#!/bin/bash

openssl req -new -x509 -days 1095 -nodes -subj "/C=CN/ST=BJ/O=EventBus Python Client/CN=localhost" -out ca.crt -keyout ca.key
