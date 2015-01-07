

var websocketCommand;
var websocketLogger;

var serialization = "TURTLE";
var currentInstanceID = 1;

var wsMotors = [];

var wsGaugeData;

var wsUriLogger;
var wsUriCommand;

var controlValueType = 0;

window.addEventListener("load", init, false);


function writeToScreen(message) {
	var pre = document.createElement("p");
	pre.style.wordWrap = "break-word";	

	pre.innerHTML = message.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, "<br />");
	
	output.appendChild(pre);

	// Scroll automatically
	output.scrollTop = output.scrollHeight;
}

function writeSeperator() {
	var preSeparator = document.createElement("p");
	preSeparator.style.wordWrap = "break-word";
	preSeparator.innerHTML = "-------------------------------------------------------------------------------------";

	output.appendChild(preSeparator);
	
	// Scroll automatically
	output.scrollTop = output.scrollHeight;
}

String.prototype.escape = function() {
	var tagsToReplace = {
		'&' : '&amp;',
		'<' : '&lt;',
		'>' : '&gt;'
	};
	return this.replace(/[&<>]/g, function(tag) {
		return tagsToReplace[tag] || tag;
	});
};

function getRootUri() {
	return "ws://" + (document.location.hostname == "" ? "localhost" : document.location.hostname) + ":" + (document.location.port == "" ? "8080" : document.location.port);
}

function init() {
	output = document.getElementById("output");

	wsUriLogger = getRootUri() + "/bus/api/logger";
	websocketLogger = new WebSocket(wsUriLogger);

	websocketLogger.onerror = function(evt) {
		writeToScreen('<span style="color: red;">Logger ERROR:</span> ' + evt.data, false);
	};
	websocketLogger.onopen = function(evt) {
		writeToScreen("Logger: connected to endpoint.", false);
	};
	websocketLogger.onclose = function(evt) {
		writeToScreen("Logger: disconnected", false);
	};
	websocketLogger.onmessage = function(evt) {
		var json = $.parseJSON(evt.data);
		if (json.hasOwnProperty("METHOD_TYPE")) {
			writeToScreen(json.METHOD_TYPE);
			writeToScreen("Target: "+json.METHOD_TARGET);
			writeToScreen(json.body);
			writeSeperator();
		}
	};
}

