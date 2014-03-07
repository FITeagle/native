function init() {
	output = document.getElementById("output");
}

function getRootUri() {
	return "ws://"
			+ (document.location.hostname == "" ? "localhost"
					: document.location.hostname) + ":"
			+ (document.location.port == "" ? "8080" : document.location.port);
}

function writeToScreen(message) {
	var pre = document.createElement("p");
	pre.style.wordWrap = "break-word";
	pre.innerHTML = message.replace(/\n/g, "<br />");

	output.appendChild(pre);
}

function send_message() {
	doSend(textID.value);
}

function doSend(message) {
	websocketCommand.send(message);
}


var wsUriLogger = getRootUri() + "/native/api/bus/logger";
websocketLogger = new WebSocket(wsUriLogger);

websocketLogger.onerror = function(evt) {
	writeToScreen('<span style="color: red;">Logger ERROR:</span> ' + evt.data);
};
websocketLogger.onopen = function(evt) {
	writeToScreen("Logger: connected to endpoint.");
};
websocketLogger.onmessage = function(evt) {
	writeToScreen(evt.data);
};

var wsUriCommand = getRootUri() + "/native/api/bus/command";
websocketCommand = new WebSocket(wsUriCommand);
websocketCommand.onopen = function(evt) {
	writeToScreen("Sender: connected to endpoint.");
};
websocketCommand.onerror = function(evt) {
	writeToScreen('<span style="color: red;">Sender ERROR:</span> ' + evt.data);
};


window.addEventListener("load", init, false);