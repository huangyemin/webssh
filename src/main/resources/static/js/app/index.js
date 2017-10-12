require([], function () {
    var term = new Terminal({
            cursorBlink: true,
            rows: 80,
            cols: 120
        }),
        protocol = (location.protocol === 'https:') ? 'wss://' : 'ws://',
        socketURL = protocol + location.hostname + ((location.port) ? (':' + location.port) : '') + "/websocket";

    var sock = new WebSocket(socketURL);
    sock.addEventListener('open', function () {
        term.attach(sock, true, false);
    });
    term.open(document.getElementById('terminal-container'));
    term.fit();
});