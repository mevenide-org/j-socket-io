'use strict';

/**
 * Module dependencies.
 */

var debug = require('debug')('java-common'),
    eio = require('./java-engine.io');

/**
 * Listen shortcut that fires a callback on an epheemal port.
 */
exports.listen = function (opts, fn) {
    if ('function' == typeof opts) {
        fn = opts;
        opts = {};
    }

    var e = eio.listen(null, opts, function(err, port) {
        if (err) {
            throw err;
        }
        fn(port);
    });

    return e;
};

/**
 * We proxy the 'on' event registration function on a socket because we have to allow the stomp event handlers
 * to run.
 *
 * TODO remove this
 *
 * @param eioc
 */
exports.proxyEngineIoClientSocket = function(eioc) {
    var originalOnFn = eioc.Socket.prototype.on;
    eioc.Socket.prototype.on = function(event, callback) {
        originalOnFn.apply(this, [event, function() {
            var that = this,
                args = arguments;
            callback.apply(that, args);
        }]);
    };
};

/**
 * Sprintf util.
 */

require('s').extend();
