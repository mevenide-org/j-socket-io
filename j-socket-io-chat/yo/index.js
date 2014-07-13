var app = require('express')();
var http = require('http').Server(app);
var io = require('socket.io')(http);

app.get('/', function(req, res){
    res.sendfile('build/static/index.html');
});

app.get('/js/3p/socket.io-client/socket.io.js', function(req, res){
    res.sendfile('build/static/js/3p/socket.io-client/socket.io.js');
});


io.on('connection', function(socket){
    console.log('a user connected');
});

http.listen(3000, function(){
    console.log('listening on *:3000');
});
