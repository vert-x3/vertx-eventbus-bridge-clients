# Vert.x / Java Sample application

This application receives and posts messages on the Vert.x event bus.

You may run it directly as:

    $ ./gradlew run

or you may prepare a distribution image with all dependencies and shell scripts:

    $ ./gradlew installDist
    $ cd build/install/vertx-sample-app
    $ bin/vertx-sample-app
