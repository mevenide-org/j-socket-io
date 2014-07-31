'use strict';

/**
 * Module dependencies.
 */

var Promise = require('bluebird'),
    _ = require('lodash'),
    debug = require('debug')('java-common'),
    eio = require('./java-engine.io.js'),
    fs = require('fs'),
    java = require('java'),
    path = require('path'),
    request = require('superagent');

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

var port = 8081,
    currentEngine;

/**
 * Listen shortcut that fires a callback on an epheemal port.
 */
exports.listen = function (opts, fn) {
    if ('function' == typeof opts) {
        fn = opts;
        opts = {};
    }

    if ('function' == typeof opts) {
        fn = opts;
        opts = {};
    }

//    var e = require('engine.io').listen(null, opts, function () {
//        fn(e.httpServer.address().port);
//    });
//
//    return e;

    var testServer = java.newInstanceSync('org.facboy.engineio.TestServer'),
        port = testServer.start(function(err, port) {
            if (err) {
                throw err;
            }
            fn(port);
        });

    afterEach(function(done) {
        console.log('fater');
        done();
    });

//    request.post('http://localhost:' + port + '/engine.io.manage/config')
//        .set('Content-Type', 'application/json')
//        .send(opts)
//        .end(function(err, res) {
//            err = getResponseError(err, res);
//            if (err) {
//                throw err;
//            } else {
//                var that = this;
//                updateClients(function(err, res) {
//                    if (err) {
//                        throw err;
//                    }
//                    fn.call(that, port);
//                });
//            }
//        });

    currentEngine = {};
    Object.defineProperty(currentEngine, 'clients', {
        enumerable: true,
        get: function() {
//            return Future.wrap(function(cb) {
//                var uri = 'http://localhost:' + port + '/engine.io.manage/clients';
//                request.get(uri)
//                    .end(function(err, res) {
//                        if (err) {
//                            console.log("Received err from '%s': %s", uri, err);
//                        }
//                        cb(err, res.body);
//                    });
//            }, 0)().wait();
            return this._clients;
        }
    });
    Object.defineProperty(currentEngine, 'clientsCount', {
        enumerable: true,
        get: function() {
            return this.clients.length;
        }
    });
    currentEngine.on = function on(event, cb) {
        request.get('http://localhost:' + port + '/engine.io.manage/events/connection')
            .end(function(err, res) {
                err = getResponseError(err, res);
                if (err) {
                    throw err;
                } else {
                    _.forEach(res.body, function(eventWrapper) {
                        "use strict";

                        cb(new eio.Socket(eventWrapper.event.sessionId, eventWrapper.event.transport));
                    });
                }
            });
    };
    return currentEngine;
};

function updateClients(cb) {
    request.get('http://localhost:' + port + '/engine.io.manage/clients')
        .end(function(err, res) {
            err = getResponseError(err, res);
            if (!err) {
                currentEngine._clients = res.body;
            }
            cb(err, res.body);
        });
}

function getResponseError(err, res) {
    if (err) {
        console.log("Received err from '%s': %s", err);
    } else if (res.error) {
        err = res.error;
        console.log(err);
    }
    return err;
}

/**
 * We proxy the 'on' event registration function on a socket because we have to use Fiber to simulate
 * a blocking call on the engine properties.
 *
 * @param eioc
 */
exports.proxyEngineIoClientSocket = function(eioc) {
    var originalOnFn = eioc.Socket.prototype.on;
    eioc.Socket.prototype.on = function(event, callback) {
        originalOnFn.apply(this, [event, function() {
            var that = this,
                args = arguments;
            // update clients
            updateClients(function(err, clients) {
                if (err) {
                    throw err;
                }
                callback.apply(that, args);
            });
        }]);
    };
};

/**
 * Sprintf util.
 */

require('s').extend();
