# SockJS Event Bus bridge for Vert.x

The `vertx-eventbus.js` file is a SockJS-based event bus bridge to connect your JavaScript frontend application to Vert
.x 3.x. You can use it in a front-end application or in a node.js application.

If you already use a previous version of the bridge, please check the changelog below. 

## Retrieving the client with NPM

The file is delivered as an NPM that you can download it using:

```
npm install --save @vertx/eventbus-bridge-client.js
```

The dependency is:

```
"@vertx/eventbus-bridge-client.js": "1.0.0"
```

More information on:

* The [vert.x web site](http://vertx.io) 
* The [Event Bus Bridge documentation](http://vertx.io/docs/vertx-web/java/#_sockjs_event_bus_bridge) 

## Releasing to webjars.org

**WebJar**

1. Go to https://www.webjars.org/
2. Click on _Add a webjar_
3. In the dialog, select `NPM` as WebJar Type
4. Enter `@vertx/eventbus-bridge-client.js` in the NPM name text area
4. Select the new version
5. Click on _Deploy_
6. Wait and check log

The sync takes some time, so you will need to check later. The log should end with "Syncing to Maven Central (this could take a while)" - or something similar.


## Changelog

### 1.0.0-2
* Fixed README

### 1.0.0-1 - 2020-10-28

* New artifact name.
