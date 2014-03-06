var results = document.getElementById("results");

function getRootUri() {
	return "ws://"
			+ (document.location.hostname == "" ? "localhost"
					: document.location.hostname) + ":"
			+ (document.location.port == "" ? "8080" : document.location.port);
}

function showError(message) {
	addResource(results, message);
}

function onError(evt) {
	showError(evt.data);
}

function onOpen(evt) {
	//
}

function onMessage(evt) {
    console.log("Parsing" + evt.data);
	data = JSON.parse(evt.data);
	uid = data.uid;
	status = data.status;
	if (null == document.getElementById(uid)) {
		addResource(results, uid);
	}

	updateStatus(uid, status);
}


try {
	var success = false;
	websocket = new WebSocket(getRootUri() + "/ws/status/json");
	websocket.onopen = function(evt) {
		success = true;
		onOpen(evt);
	};
	websocket.onmessage = function(evt) {
		onMessage(evt);
	};
	websocket.onerror = function(evt) {
		onError(evt);
	};
	websocket.onclose = function() {
		if (!success)
			showError("connection error");
	};
} catch (e) {
	showError(e.message);
}