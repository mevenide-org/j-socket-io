'use strict';

var Promise = require('bluebird'),
    _ = require('lodash'),
    debug = require('debug')('java-engine.io'),
    fs = require('fs'),
    java = require('java'),
    path = require('path'),
    Stomp = require('stompjs');

(function() {
    java.options.push('-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005');

    java.classpath.push('../target/classes');
    java.classpath.push('../target/test-classes');

// add the maven classpath. not very efficient, but meh
    var classpathList = fs.readFileSync('classpath.list', {encoding: 'utf8'});
    _.forEach(classpathList.split(/\r?\n/), function(file) {
        if (file) {
            debug('adding ' + file + ' to classpath');
            java.classpath.push(file);
        }
    });

})();

module.exports = {
    listen: listen,
    Socket: Socket
};

var testServer;
afterEach(function(done) {
    if (testServer) {
        var myServer = testServer;
        testServer = null;
        myServer.stop(function (err) {
            if (err) {
                throw err;
            }
            done();
        })
    } else {
        done();
    }
});

function listen(port, options, fn) {
    if ('function' == typeof options) {
        fn = options;
        options = {};
    }

    var config = java.newInstanceSync('java.util.HashMap');
    _.forOwn(options, function(value, key) {
        config.putSync(key, value);
    });

    var myServer;
    java.newInstance('org.facboy.engineio.TestServer', port, config, function(err, javaServer) {
        if (err) {
            fn(err);
        }

        myServer = testServer = new TestServer(javaServer);
        myServer.start(function(err, port) {
            myServer.port = port;

            fn(err, port);
        });
    });

    var engine = {};
    Object.defineProperty(engine, 'clients', {
        enumerable: true,
        get: function() {
            if (myServer) {
                return myServer.clients;
            }
            return [];
        }
    });
    Object.defineProperty(engine, 'clientsCount', {
        enumerable: true,
        get: function() {
            return this.clients.length;
        }
    });
    engine.on = function on(event, fn) {
        switch (event) {
            case 'connection':
                myServer.stompClient.subscribe("/topic/connections", function(message) {
                    var event = JSON.parse(message.body);
                    fn(new Socket(myServer, event.sessionId, {name: event.transport}));
                });
        }
    };
    return engine;
}

function TestServer(javaServer) {
    this.server = javaServer;

    Object.defineProperty(this, 'clients', {
        enumerable: true,
        get: function() {
            return javaServer.getTestEngineIoSync().getClientsSync().toArraySync();
        }
    });
}
TestServer.prototype.stop = function stop(fn) {
    this.server.stop(function (err) {
        fn(err);
    });
    if (this.stompClient) {
        this.stompClient.disconnect();
    }
};
TestServer.prototype.start = function start(fn) {
    var that = this;
    this.server.start(function(err, port) {
        that.port = port;
        if (!err) {
            var client = that.stompClient = Stomp.overWS('ws://localhost:' + port + '/springws');
//            client.debug = function(str) {
//                console.log(str);
//            };
            client.connect({},
                function() {
                    fn(null, port);
                },
                function(err) {
                    fn(err)
                });
        } else {
            fn(err);
        }
    });
};

function Socket(testServer, id, transport) {
    this.testServer = testServer;
    this.id = id;
    this.transport = transport;
}
Socket.prototype.on = function on(event, fn) {
    switch (event) {
        case 'message':
            this.testServer.stompClient.subscribe('/topic/sockets/' + this.id + '/messages', function (message) {
                var event = JSON.parse(message.body);
                fn(event);
            });
            break;
    }
};
Socket.prototype.send = function send(data) {
    this.testServer.stompClient.send('/app/sockets/' + this.id, {}, JSON.stringify({
        data: data
    }));
};
