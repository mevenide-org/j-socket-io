'use strict';

var http = require('http');
http.get('http://localhost:8080/socket.io/?EIO=2&transport=polling&t=1405190051956-62', function(res) {
    res.on('data', dataListener)
});

function dataListener(chunk) {
    console.log('got %d bytes of data', chunk.length);
    console.log('%d %d %d %d', chunk[0], chunk[1], chunk[2], chunk[3]);
    console.log(chunk.toString('hex'));
    console.log(chunk.toString('utf-8'));
    process.exit();
}
