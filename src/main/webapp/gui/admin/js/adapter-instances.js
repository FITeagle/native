var REST_HOST = "http://localhost:8080/native/api/resources/garage/";

var DRAW_CHART = true;

var websocketLogger;

var serialization = "TURTLE";
var currentInstanceID = 1;

var wsMotors = [];

var wsGaugeData;

var wsUriLogger;
var wsUriCommand;

var controlValueType = 0;

var chart;

window.addEventListener("load", init, false);

google.load('visualization', '1', {
	packages : [ 'gauge' ]
});

function send_message() {
	sendMessageViaWs(textID.value);
}

function sendMessageViaWs(message) {
	writeToScreen("Sending message : " + message, false);
	websocketCommand.send(message);
}

var gaugeOptions = {
	// width: 400,
	height : 200,
	min : 0,
	max : 1000,
	redFrom : 900,
	redTo : 1000,
	yellowFrom : 750,
	yellowTo : 900,
	minorTicks : 50,
	animation : {
		duration : 2000,
		easing : 'out',
	},
};

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

	wsUriLogger = getRootUri() + "/bus/api/logger";
	websocketLogger = new WebSocket(wsUriLogger);

	websocketLogger.onerror = function(evt) {
		console.log('<span style="color: red;">Logger ERROR:</span> ' + evt.data, false);
	};
	websocketLogger.onopen = function(evt) {
		console.log("Logger: connected to endpoint.", false);
	};
	websocketLogger.onclose = function(evt) {
		console.log("Logger: disconnected", false);
	};
	websocketLogger.onmessage = function(evt) {
		if (evt.data.indexOf("Event Notification:") > -1) {
			// console.log(evt.data);
			processEvent(evt.data);
			if (DRAW_CHART) {
				drawChart();
			}
		}
		// console.log(evt.data, true);
	};

	if (DRAW_CHART) {
		initGoogleCharts();
		restGetInstances();
	}
}

function restGetInstances() {
	var restURL = REST_HOST;
	restGETInstances(restURL);
}

function restGETInstances(restURL) {
	console.log("Sending GET " + restURL);
	$.ajax({
		url : restURL,
		type : 'GET',
		// data : 'ID=1&Name=John&Age=10', // or $('#myform').serializeArray()
		success : function(data) {
			console.log("Received RDF data!");
			processGetInstances(data);
		}
	});
}

function initGoogleCharts() {

	wsGaugeData = new google.visualization.DataTable();
	wsGaugeData.addColumn('string', 'Motor');
	wsGaugeData.addColumn('number', 'RPM');

	chart = new google.visualization.Gauge(document.getElementById('wsGraphics'));
}

function drawChart() {
	chart.draw(wsGaugeData, gaugeOptions);
}

function processEvent(ttlString) {

	if (ttlString.indexOf("terminated:") > 0) {
		var pos = ttlString.indexOf("terminated:");
		var pos2 = ttlString.indexOf(";;");
		var instanceIdToTerminate = ttlString.slice(pos + 11, pos2);

		for ( var i = 0; i < wsGaugeData.getNumberOfRows(); i++) {
			if (wsGaugeData.getValue(i, 0) == instanceIdToTerminate) {
				wsGaugeData.removeRow(i);
			}
		}

	} else if (ttlString.indexOf("provisioned:") > 0) {
		var pos = ttlString.indexOf("provisioned:");
		var pos2 = ttlString.indexOf("::");
		var instanceIdToProvision = ttlString.slice(pos + 12, pos2);
		var pos3 = ttlString.indexOf(";;");
		var rpmToProvision = parseInt(ttlString.slice(pos2 + 2, pos3));
		wsGaugeData.addRow([ instanceIdToProvision, rpmToProvision ]);

	} else if (ttlString.indexOf("changedRPM:") > 0) {
		var pos = ttlString.indexOf("changedRPM:");
		var pos2 = ttlString.indexOf("::");
		var instanceIdToChange = ttlString.slice(pos + 11, pos2);
		var pos3 = ttlString.indexOf(";;");
		var rpmToChange = parseInt(ttlString.slice(pos2 + 2, pos3));

		for ( var i = 0; i < wsGaugeData.getNumberOfRows(); i++) {
			if (wsGaugeData.getValue(i, 0) == instanceIdToChange) {
				wsGaugeData.setValue(i, 1, rpmToChange);
			}
		}
	}
}

function processGetInstances(ttlString) {

	// var pos = ttlString.indexOf("* result :");
	// ttlString = ttlString.slice(pos + 10);

	var index = 0;

	var parser = N3.Parser();

	wsMotors = [];
	parser.parse(ttlString, function(error, triple, prefixes) {
		if (triple) {
			if (triple.predicate == "http://fiteagle.org/ontology/adapter/motor#rpm") {
				console.log(triple.subject);
				pos = triple.subject.indexOf("#");
				var currentInstanceID = triple.subject.slice(pos + 1);
				pos = triple.object.indexOf("\"^^");
				var currentRPM = triple.object.slice(1, pos);

				var motor = {
					instanceID : currentInstanceID,
					rpm : currentRPM
				};

				wsMotors[index] = motor;
				index++;
			}
		} else {
			console.log("refresh");
			refreshGaugeData();
			if (DRAW_CHART) {
				drawChart();
			}
		}
	});

}

function refreshGaugeData() {
	wsGaugeData = new google.visualization.DataTable();
	wsGaugeData.addColumn('string', 'Motor');
	wsGaugeData.addColumn('number', 'RPM');

	for ( var index = 0; index < wsMotors.length; ++index) {
		wsGaugeData.addRow([ wsMotors[index].instanceID, parseInt(wsMotors[index].rpm) ]);
	}
}

function formatInput(inputString) {

	if (restSerialization != "ttl") {
		inputString = inputString.escape();
	}
	inputString = inputString.split('\n').join('<br/>');

	return inputString;
}
