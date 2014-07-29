
/**
 * Module dependencies.
 */

var child_process = require('child_process'),
    path = require('path'),
    Fiber = require('fibers'),
    Future = require('fibers/future'),
    request = require('superagent'),
    eio = require('./java-engine.io.js');

/**
 * Listen shortcut that fires a callback on an epheemal port.
 */

var port = 8081,
    currentEngine;

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

    request.post('http://localhost:' + port + '/engine.io.manage/config')
        .send(opts)
        .end(function(err, res) {
            err = getResponseError(err, res);
            if (err) {
                throw err;
            } else {
                var that = this;
                updateClients(function(err, res) {
                    if (err) {
                        throw err;
                    }
                    fn.call(that, port);
                });
            }
            // need to use fibers because we need to simulate blocking calls b/c of engine properties (eg engine.clients)
//            Fiber(function() {
//                fn(port);
//            }).run();
        });

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
                    cb(new eio.Socket(res.body.sessionId, res.body.transport));
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
