var host = "http://localhost:8080/AdapterMotor/api/";

var wsUri = getRootUri() + "/AdapterMotor/websocket";
//var restOutput;
//var wsOutput;
var websocket;

var serialization = "TURTLE";
//var restSerialization = "TURTLE";

var currentInstanceID = 1;

//var restMotors = [];
var wsMotors = [];

//google.load('visualization', '1', {
//	packages : [ 'gauge' ]
//});

// google.setOnLoadCallback(drawChart);

var wsGaugeData;

var wsUriLogger;
var wsUriCommand;



function writeToScreen(message, isIncoming) {
	var pre = document.createElement("p");
	pre.style.wordWrap = "break-word";
	pre.innerHTML = message.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/\n/g, "<br />");

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
    animation:{
        duration: 30000,
        easing: 'out',
      }

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
	//restOutput = document.getElementById("restOutput");
	//wsOutput = document.getElementById("wsOutput");

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
		if(evt.data.indexOf("Event Notification:") > -1){
			//wsRefreshInstanceGraphics(evt.data, true);
		} else {
			//wsRefreshInstanceGraphics(evt.data, false);
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

	
	//wsGaugeData = new google.visualization.DataTable();
	//wsGaugeData.addColumn('string', 'Motor');
	//wsGaugeData.addColumn('number', 'RPM');

	//refresGUIInstanceIDs();
}

function drawChart(data, element) {
	// var data = google.visualization.arrayToDataTable([
	// ['Label', 'Value'],
	// ['Memory', 80],
	// ['CPU', 55],
	// ['Network', 68]
	// ]);

	// var data = new google.visualization.DataTable();
	// data.addColumn('string', 'Motor');
	// data.addColumn('number', 'RPM');
	// data.addRow(['V', 200]);

	var chart = new google.visualization.Gauge(document.getElementById(element));
	chart.draw(data, gaugeOptions);
}


function wsGetDescription() {
	sendMessageViaWs("request:::description,,,serialization:::" + serialization);
}

function wsGetInstances() {
	sendMessageViaWs("request:::listResources,,,serialization:::" + serialization);
}

function wsProvisionInstance() {
	currentInstanceID = parseInt($("#wsInstanceNumber").val());
	sendMessageViaWs("request:::provision,,,instanceID:::" + currentInstanceID);
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


function onMessage(evt) {
	wsWriteToScreen(false, "Message Received: <br/>" + evt.data);
	
	if(evt.data.indexOf("Event Notification:") > -1){
		wsRefreshInstanceGraphics(evt.data, true);
	} else {
		wsRefreshInstanceGraphics(evt.data, false);
	}
}


function wsRefreshInstanceGraphics(ttlString, isEvent) {
	wsMotors = [];
	
	if(isEvent){
		if(ttlString.indexOf("terminated:") > 0){
			var pos = ttlString.indexOf("terminated:");
			var pos2 = ttlString.indexOf(";;");
			var instanceIdToTerminate = ttlString.slice(pos+11,pos2);
			
			for(var i = 0; i < wsGaugeData.getNumberOfRows(); i++){
				if(wsGaugeData.getValue(i, 0) == instanceIdToTerminate){
					wsGaugeData.removeRow(i);
				}
			}
			
			
		} else if(ttlString.indexOf("provisioned:") > 0){
			var pos = ttlString.indexOf("provisioned:");
			var pos2 = ttlString.indexOf("::");
			var instanceIdToProvision = ttlString.slice(pos+12,pos2);
			var pos3 = ttlString.indexOf(";;");
			var rpmToProvision = parseInt(ttlString.slice(pos2+2,pos3));
			wsGaugeData.addRow([ instanceIdToProvision, rpmToProvision ]);
			
			
		} else if(ttlString.indexOf("changedRPM:") > 0){
			var pos = ttlString.indexOf("changedRPM:");
			var pos2 = ttlString.indexOf("::");
			var instanceIdToChange = ttlString.slice(pos+11,pos2);
			var pos3 = ttlString.indexOf(";;");
			var rpmToChange = parseInt(ttlString.slice(pos2+2,pos3));
			
			for(var i = 0; i < wsGaugeData.getNumberOfRows(); i++){
				if(wsGaugeData.getValue(i, 0) == instanceIdToChange){
					//wsGaugeData.removeRow(i);
					wsGaugeData.setValue(i, 1, rpmToChange);
				}
			}
			
			//var newValue = 1000 - data.getValue(0, 1);
			//wsGaugeData.setValue(0, 1, rpmToChange);
		    //  drawChart();

			//wsGaugeData.addRow([ "M" + instanceIdToProvision, rpmToChange ]);
			
		}
		
	} else {

		if (wsSerialization == "ttl") {
			parseTTL(ttlString, wsMotors);
		}
	
		wsGaugeData = new google.visualization.DataTable();
		wsGaugeData.addColumn('string', 'Motor');
		wsGaugeData.addColumn('number', 'RPM');
	
		for ( var index = 0; index < wsMotors.length; ++index) {
			// $("#restGraphics").append(restMotors[index].instanceID + " -> " +
			// restMotors[index].rpm + "<br/>");
			wsGaugeData.addRow([ wsMotors[index].instanceID, parseInt(wsMotors[index].rpm) ]);
		}
	}

	drawChart(wsGaugeData, 'wsGraphics');

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
}

function restRefreshInstanceGraphics(ttlString) {
	restMotors = [];

	if (restSerialization == "ttl") {
		parseTTL(ttlString, restMotors);
	}

	var data = new google.visualization.DataTable();
	data.addColumn('string', 'Motor');
	data.addColumn('number', 'RPM');
	// data.addRow(['Motor 1', 200]);

	// $("#restGraphics").html("");
	for ( var index = 0; index < restMotors.length; ++index) {
		// $("#restGraphics").append(restMotors[index].instanceID + " -> " +
		// restMotors[index].rpm + "<br/>");
		data.addRow([ "M" + restMotors[index].instanceID + " RPM", parseInt(restMotors[index].rpm) ]);
	}

	drawChart(data, 'restGraphics');
}

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



function setSerialization(fileEnding) {
	serialization = fileEnding;
}

function setWsSerialization(fileEnding) {
	wsSerialization = fileEnding;
}


window.addEventListener("load", init, false);