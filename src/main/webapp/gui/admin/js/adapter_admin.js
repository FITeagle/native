var REST_HOST = "http://localhost:8080/AdapterMotor/api/";

var DRAW_CHART = false;

var websocketCommand;
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

	//google.load('visualization', '1', {
	//	packages : [ 'gauge' ]
	//});Please stop "improving" the motor adapter code without even getting in touch with me. 

function writeToScreen(data, isIncoming) {
	var pre = document.createElement("p");
	pre.style.wordWrap = "break-word";	
		
	var json = $.parseJSON(data);

	if (json.hasOwnProperty("rdf") && json.hasOwnProperty("method_type")) {

		var message = json.rdf;
		pre.innerHTML = message.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, "<br />");
	
		if (!isIncoming) {
			pre.style.color = "white";
		}
	
		output.appendChild(pre);
	
		// Insert separator line
		var preSeparator = document.createElement("p");
		preSeparator.style.wordWrap = "break-word";
		preSeparator.innerHTML = "-------------------------------------------------------------------------------------";
	
		output.appendChild(preSeparator);
		// Scroll automatically
		output.scrollTop = output.scrollHeight;
}
}

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
		if (evt.data.indexOf("Event Notification:") > -1) {
			//processEvent(evt.data);
			if (DRAW_CHART) {
			//	drawChart();
			}
		} else if (evt.data.indexOf("response : listResources") > -1 && evt.data.indexOf("rdfs:label") > -1) {
			//processGetInstances(evt.data);
		}
		writeToScreen(evt.data, true);
	};

	wsUriCommand = getRootUri() + "/bus/api/commander";
	websocketCommand = new WebSocket(wsUriCommand);
	websocketCommand.onopen = function(evt) {
		writeToScreen("Sender: connected to endpoint.", false);
	};
	websocketCommand.onerror = function(evt) {
		writeToScreen('<span style="color: red;">Sender ERROR:</span> ' + evt.data, false);
	};

	refreshGUIInstanceIDs();
	clearControlInput();

	if (DRAW_CHART) {
		initGoogleCharts();
	}
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

function wsGetDescription() {
	sendMessageViaWs("request:::description,,,serialization:::" + serialization);
}

function wsGetInstances() {
	sendMessageViaWs("request:::listResources,,,serialization:::" + serialization);
}

function wsProvisionInstance() {
	currentInstanceID = parseInt($("#wsInstanceNumber").val());
	sendMessageViaWs("method_type:::type_create,,,instanceID:::" + currentInstanceID);
	currentInstanceID++;
	refreshGUIInstanceIDs();
}

function wsMonitorInstance() {
	currentInstanceID = parseInt($("#wsInstanceNumber").val());
	sendMessageViaWs("request:::monitor,,,instanceID:::" + currentInstanceID + ",,,serialization:::" + serialization);
	refreshGUIInstanceIDs();
}

function wsTerminateInstance() {
	currentInstanceID = parseInt($("#wsInstanceNumber").val());
	sendMessageViaWs("request:::terminate,,,instanceID:::" + currentInstanceID);
	refreshGUIInstanceIDs();
}

function wsControlInstances() {
	currentInstanceID = parseInt($("#wsInstanceNumber").val());
	var controlInput = $("#controlInput").val();
	sendMessageViaWs("request:::control,,,control:::" + controlInput + ",,,serialization:::" + serialization);
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
	if (serialization == "TURTLE") {

		var pos = ttlString.indexOf("* result :");
		ttlString = ttlString.slice(pos + 10);

		var index = 0;

		var parser = N3.Parser();

		wsMotors = [];
		parser.parse(ttlString, function(error, triple, prefixes) {
			if (triple) {
				if (triple.predicate == "http://fiteagle.org/ontology/adapter/motor#rpm") {

					pos = triple.subject.indexOf("/motor#m");
					var currentInstanceID = triple.subject.slice(pos + 8);
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
				refreshGaugeData();
				if (DRAW_CHART) {
					drawChart();
				}
			}
		});

	}

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

function refreshGUIInstanceIDs() {
	$("#wsInstanceNumber").val(currentInstanceID);
	$("#controlInstanceNumber").val(currentInstanceID - 1);
}

/*
function parseTTL(ttlString, motors) {

	var index = 0;

	var pos = ttlString.indexOf("rdfs:label");
	while (pos > 0) {

		ttlString = ttlString.slice(pos);
		pos = ttlString.indexOf("\"");
		ttlString = ttlString.slice(pos + 1);
		pos = ttlString.indexOf("\"");
		var currentInstanceID = ttlString.slice(0, pos);

		pos = ttlString.indexOf(":rpm");
		ttlString = ttlString.slice(pos);
		pos = ttlString.indexOf("\"");
		ttlString = ttlString.slice(pos + 1);
		pos = ttlString.indexOf("\"");
		var currentRPM = ttlString.slice(0, pos);

		var motor = {
			instanceID : currentInstanceID,
			rpm : currentRPM
		};

		motors[index] = motor;

		index++;
		pos = ttlString.indexOf("rdfs:label");
	}
}

*/

function setSerialization(fileEnding) {
	serialization = fileEnding;
}

function setWsSerialization(fileEnding) {
	wsSerialization = fileEnding;
}

function generateControlCode() {

	var code = "@prefix :      <http://fiteagle.org/ontology/adapter/motor#> .";
	code += "\n";
	code += "@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .";
	code += "\n";
	code += "@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .";
	code += "\n";
	code += ":m" + $("#controlInstanceNumber").val();
	code += "	a             :MotorResource ;"
	code += "\n";
	code += "			rdfs:label \"" + $("#controlInstanceNumber").val() + "\" ;";
	code += "\n";
	code += "			:" + $("#controlProperty").val() + " \"";
	if (controlValueType == 1) {
		code += $("#controlValueBoolean").val() + "\"^^xsd:boolean .";
	} else if (controlValueType == 0) {
		code += $("#controlValueInteger").val() + "\"^^xsd:long .";
	}

	$("#controlInput").val(code);
}

function refreshControlValueType() {

	if ($("#controlProperty").val() == "isDynamic") {
		$("#controlValueInteger").hide();
		$("#controlValueBoolean").show();
		controlValueType = 1;
	} else {
		$("#controlValueInteger").show();
		$("#controlValueBoolean").hide();
		controlValueType = 0;
	}
}

function clearControlInput() {
	$("#controlInput").val("");
	var element = document.getElementById('controlProperty');
	element.value = 'rpm';

	$("#controlValueInteger").val("");
	$("#controlValueBoolean").val("true");

	$("#controlValueInteger").show();
	$("#controlValueBoolean").hide();
}
