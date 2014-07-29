module.exports = {
    Socket: Socket
};

function Socket(id, transport) {
    this.id = id;
    this.transport = transport;
}
