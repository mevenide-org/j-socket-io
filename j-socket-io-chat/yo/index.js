var express = require('express'),
    http = require('http'),
    socketio = require('socket.io');


var app = express(),
    server = http.Server(app),
    io = socketio(server);

app.get('/', function(req, res){
    res.sendfile('build/static/index.html');
});

app.use(express.static(__dirname + '/build/static/'));

app.get('/js/3p/socket.io-client/socket.io.js', function(req, res){
    res.sendfile('build/static/js/3p/socket.io-client/socket.io.js');
});

io.on('connection', function(socket){
    console.log('a user connected');
    socket.on('disconnect', function(){
        console.log('user disconnected');
    });
    socket.on('chat message', function(msg){
        console.log('message: ' + msg);
        io.emit('chat message', msg);
    });
});

server.listen(3000, function(){
    console.log('listening on *:3000');
});
