

//var adapterName = "ADeployedMotorAdapter1";
var adapterName = "";
var adapterOntologyPrefix = "";
var adapterOntology = "";
var adapterResourceName = "";
var adapterType = "";

var gotAdapterParams = false;
var finishedInit = false;


var FITEAGLE_INTERNAL = "http://fiteagleinternal#";
var ONTOLOGY_FITEAGLE_RELEASES = "http://fiteagle.org/ontology#releases";
var ONTOLOGY_RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
var ONTOLOGY_FITEAGLE_MESSAGE = FITEAGLE_INTERNAL + "Message";

var BASE_URL = "http://localhost:8080/native/api/resources/";
var LOGGER_SERVICE_URL = "/bus/api/logger";

var METHOD_TYPE_INFORM = "type_inform";

var websocketLogger;
var wsUriLogger;

var resourceInstances = [];
var resourceInstanceBeforeConfigure;

var adapterInstances = [];

window.addEventListener("load", init, false);

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

function getURLParameter(name) {
  return decodeURIComponent((new RegExp('[?|&]' + name + '=' + '([^&;]+?)(&|#|;|$)').exec(location.search)||[,""])[1].replace(/\+/g, '%20'))||null
}

function getRootUri() {
	return "ws://" + (document.location.hostname == "" ? "localhost" : document.location.hostname) + ":" + (document.location.port == "" ? "8080" : document.location.port);
}

function init() {

	wsUriLogger = getRootUri() + LOGGER_SERVICE_URL;
	websocketLogger = new WebSocket(wsUriLogger);

	websocketLogger.onerror = function(evt) {
		console.log('<span style="color: red;">Logger ERROR:</span> ' + evt.data);
	};
	websocketLogger.onopen = function(evt) {
		console.log("Logger: connected to endpoint.");
	};
	websocketLogger.onclose = function(evt) {
		console.log("Logger: disconnected");
	};
	websocketLogger.onmessage = function(evt) {
		var json = $.parseJSON(evt.data);

		if(finishedInit){
			if (json.hasOwnProperty("rdf") && json.hasOwnProperty("method_type")) {
				if (json.method_type === METHOD_TYPE_INFORM) {
					processInform(json.rdf);
				}
			}
		}
	};

	adapterName = getURLParameter('adaptername');
	if(adapterName && adapterName !== ""){
		restGetInitList();
	} else {
		restGetTestbedAdapterList();
	}
}

//function restGetInstances() {
//	var restURL = BASE_URL + adapterName;
//	var callback = function(data) {
//		console.log("Received Instances RDF data!");
//		processGetInstances(data);
//	};
//	restGET(restURL, callback);
//}

function restGetTestbedAdapterList() {
	var restURL = BASE_URL;
	var callback = function(data) {
		console.log("Received Testbed Adapter RDF data!");
		processGetTestbedAdapters(data);
	};
	restGET(restURL, callback);
}

function restGetInitList() {
	var restURL = BASE_URL + adapterName;
	var callback = function(data) {
		console.log("Received Adapter RDF data!");
		resourceInstances = [];
		processGetAdapterParameters(data);
	};
	restGET(restURL, callback);
}

function restReleaseInstance(instanceName) {
	var restURL = BASE_URL + adapterName + "/" + instanceName;
	restDELETE(restURL);
}

function restConfigureInstances(configureTTL) {
	if(gotAdapterParams){
		var restURL = BASE_URL + adapterName;
		restPOST(restURL, configureTTL);
	} else {
		alert("Adapter not setup properly");
	}
}

function restCreateInstance(createTTL) {
	if(gotAdapterParams){
		var restURL = BASE_URL + adapterName;
		restPUT(restURL, createTTL);
	} else {
		alert("Adapter not setup properly");
	}
}

function restPOST(restURL, dataToSend) {
	console.log("Sending POST " + restURL);
	$.ajax({
		url : restURL,
		type : 'POST',
		contentType : "application/x-www-form-urlencoded",
		data : "" + dataToSend,
		success : function(data) {
			//processInform(data);
			// Done automatically by processInform
		}
	});
}

function restPUT(restURL, dataToSend) {
	console.log("Sending PUT " + restURL);
	$.ajax({
		url : restURL,
		type : 'PUT',
		contentType : "application/x-www-form-urlencoded",
		data : "" + dataToSend,
		success : function(data) {
			//processCreate(data);
			// Done automatically by processInform
		}
	});
}

function restDELETE(restURL) {
	console.log("Sending DELETE " + restURL);
	$.ajax({
		url : restURL,
		type : 'DELETE',
		success : function(data) {
			//processDelete(data);
			// Done automatically by processInform
		}
	});
}

function restGET(restURL, callback) {
	console.log("Sending GET " + restURL);
	$.ajax({
		url : restURL,
		type : 'GET',
		success : callback
	});
}

//function processDelete(ttlString) {
//
//	var parser = N3.Parser();
//	parser.parse(ttlString, function(error, triple, prefixes) {
//		if (triple) {
//			for ( var index = 0; index < resourceInstances.length; ++index) {
//				if (triple.object === resourceInstances[index].name_full) {
//					guiRemoveResourceInstance(resourceInstances[index].name);
//					resourceInstances.splice(index, 1);
//					break;
//				}
//			}
//		}
//	});
//
//}

function processGetAdapterParameters(ttlString) {

	var adapterParameters = {};
	var currentProperty = "";

	var parser = N3.Parser();
	parser.parse(ttlString, function(error, triple, prefixes) {
		if (triple) {
			// Find adapter resource instance type (fitealge:implements), prefixs
			if(triple.predicate === "http://fiteagle.org/ontology#implements"){
				
				var posPrefixObj = triple.object.indexOf("#");
				var posPrefixSubj = triple.subject.indexOf("#");
				var prefix = triple.object.slice(0, posPrefixObj+1);
				
				adapterOntologyPrefix = prefix.slice(prefix.lastIndexOf("/") + 1, prefix.length - 1);
				adapterOntology = prefix;
				adapterResourceName = triple.object;
				adapterType = triple.subject;
				
				
				console.log("Current adapter path :" + adapterOntologyPrefix);
				console.log("Current adapter prefix: " + adapterOntology);
				console.log("Current adapter resource type name: " + adapterResourceName);
	
				gotAdapterParams = true;
			}
		} else {
			guiRefreshLabels();
			processGetAdapterProperties(ttlString);
		}
	});
}

function processGetAdapterProperties(ttlString) {

	var adapterParameters = {};
	var currentProperty = "";

	var parser = N3.Parser();
	parser.parse(ttlString, function(error, triple, prefixes) {
		if (triple) {			
			// Find Resource Instance Type
			if (triple.predicate === "http://www.w3.org/2000/01/rdf-schema#domain" && triple.object === adapterResourceName) {
				currentProperty = triple.subject;
			}

			// Find Resource Instance Type's properties
			if (triple.predicate === "http://www.w3.org/2000/01/rdf-schema#range" && triple.subject === currentProperty) {
				var posPrefixSubj = triple.subject.indexOf("#");
				var propertyName = triple.subject.slice(posPrefixSubj + 1);
				var posPrefixObj = triple.object.indexOf("#");
				var propertyDataType = triple.object.slice(posPrefixObj + 1);
				adapterParameters[propertyName] = propertyDataType;
			}

		} else {
			guiAddCreateBox(adapterParameters);
			
			processInform(ttlString);
			//processGetInstances(ttlString);
		}
	});

}


function processGetTestbedAdapters(ttlString) {

	adapterInstances = [];

	var parser = N3.Parser();
	parser.parse(ttlString, function(error, triple, prefixes) {
		if (triple) {
			// Find adapter resource instance type (fitealge:implements), prefixs
			if(triple.subject.indexOf(FITEAGLE_INTERNAL) != -1 && triple.predicate === ONTOLOGY_RDF_TYPE && triple.subject !== ONTOLOGY_FITEAGLE_MESSAGE){
				
				var posPrefixSubj = triple.subject.indexOf("#");

				var currentAdapter = {};
				currentAdapter['name'] = triple.subject.slice(posPrefixSubj + 1);
				currentAdapter['type'] = triple.object;		
				adapterInstances.push(currentAdapter);
			}
		} else {
			guiRefreshAdaptersList();
		}
	});
}


//function processGetInstances(ttlString) {
//
//	var index = 0;
//
//	resourceInstances = [];
//
//	var currentResInstance = {};
//
//	var parser = N3.Parser();
//	parser.parse(ttlString, function(error, triple, prefixes) {
//		if (triple) {
//			if (triple.predicate === "http://www.w3.org/1999/02/22-rdf-syntax-ns#type" && triple.object === adapterResourceName) {
//				if (!isEmpty(currentResInstance)) {
//					resourceInstances[index] = currentResInstance;
//					index++;
//				}
//
//				var posPrefixSubj = triple.subject.indexOf("#");
//
//				currentResInstance = {};
//				currentResInstance['name_full'] = triple.subject;
//				currentResInstance['name'] = triple.subject.slice(posPrefixSubj + 1);
//			}
//			if (currentResInstance.name_full === triple.subject) {
//				var posPrefixPred = triple.predicate.indexOf("#");
//				currentResInstance[triple.predicate.slice(posPrefixPred + 1)] = getObjectValue(triple);
//			}
//		} else {
//			if (!isEmpty(currentResInstance)) {
//				resourceInstances[index] = currentResInstance;
//				index++;
//			}
//			guiRefreshList();
//		}
//	});
//}

//function processCreate(ttlString) {
//
//	var index = 0;
//
//	resourceInstances = [];
//
//	var currentResInstance = {};
//
//	var parser = N3.Parser();
//	parser.parse(ttlString, function(error, triple, prefixes) {
//		if (triple) {
//			if (triple.predicate === "http://www.w3.org/1999/02/22-rdf-syntax-ns#type" && triple.object === adapterResourceName) {
//				var posPrefixSubj = triple.subject.indexOf("#");
//
//				currentResInstance = {};
//				currentResInstance['name_full'] = triple.subject;
//				currentResInstance['name'] = triple.subject.slice(posPrefixSubj + 1);
//			}
//			if (currentResInstance.name_full === triple.subject) {
//				var posPrefixPred = triple.predicate.indexOf("#");
//				currentResInstance[triple.predicate.slice(posPrefixPred + 1)] = getObjectValue(triple);
//			}
//		} else {
//			if (!isEmpty(currentResInstance)) {
//				resourceInstances[resourceInstances.length] = currentResInstance;
//			}
//			guiRefreshList();
//		}
//	});
//
//}

function processInform(ttlString) {

	var parser = N3.Parser();
	parser.parse(ttlString, function(error, triple, prefixes) {

		if (triple) {
			if(triple.subject === FITEAGLE_INTERNAL + adapterName && triple.object === adapterType){
				processAdapterInstances(ttlString);
				return;
			}
		}
	});
}


function processAdapterInstances(ttlString){
	
	
	var foundResourceInstance = false;
	var instancesToAdd = [];
	
	//resourceInstances = [];

	//var currentResInstance = {};

	var parser = N3.Parser();
	parser.parse(ttlString, function(error, triple, prefixes) {

		if (triple) {
			// Is release message
			if(triple.predicate === ONTOLOGY_FITEAGLE_RELEASES){
				for ( var index = 0; index < resourceInstances.length; ++index) {
					if (triple.object === resourceInstances[index].name_full) {
						guiRemoveResourceInstance(resourceInstances[index].name);
						resourceInstances.splice(index, 1);
						break;
					}
				}
			} else {
				
				// Is configure message
				for ( var index = 0; index < resourceInstances.length; ++index) {
					if (triple.subject === resourceInstances[index].name_full) {
						var posPrefixPred = triple.predicate.indexOf("#");
						var property = triple.predicate.slice(posPrefixPred + 1);
						var value = getObjectValue(triple);
						resourceInstances[index][property] = value;
						guiRefreshProperty(resourceInstances[index].name, property, value);
						foundResourceInstance = true;
						break;
					}
				}
				
				// Is create message
				if(!foundResourceInstance){
					instancesToAdd.push(triple.subject);
				}
			}

		} else {
			processAddNewInstances(ttlString, instancesToAdd);
		}
	});
}

function processAddNewInstances(ttlString, instancesToAdd) {

	var currentResInstance = {};
	var addedInstance = false;

	var parser = N3.Parser();
	parser.parse(ttlString, function(error, triple, prefixes) {
		if (triple) {
			if (triple.predicate === "http://www.w3.org/1999/02/22-rdf-syntax-ns#type" && triple.object === adapterResourceName) {
				if (!isEmpty(currentResInstance)) {
					resourceInstances.push(currentResInstance);
				}

				var posPrefixSubj = triple.subject.indexOf("#");
				
				for ( var index = 0; index < instancesToAdd.length; ++index) {
					if(instancesToAdd[index] == triple.subject){
						addedInstance = true;
						currentResInstance = {};
						currentResInstance['name_full'] = triple.subject;
						currentResInstance['name'] = triple.subject.slice(posPrefixSubj + 1);
						break;
					}
				}
			}
			if (!isEmpty(currentResInstance) && currentResInstance.name_full === triple.subject) {
				var posPrefixPred = triple.predicate.indexOf("#");
				currentResInstance[triple.predicate.slice(posPrefixPred + 1)] = getObjectValue(triple);
			}
		} else {
			if (!isEmpty(currentResInstance)) {
				resourceInstances.push(currentResInstance);
			}
			if(!finishedInit){
				finishedInit = true;
			}
			if(addedInstance){
				guiRefreshList();
			}
		}
	});
}



function getObjectValue(triple) {
	var posPrefixObj = triple.object.indexOf("#");
	var posTypeObj = triple.object.indexOf("\"^^");

	if (posTypeObj === -1) {
		var posQuotation = triple.object.indexOf("\"");
		if (posQuotation === -1) {
			return triple.object.slice(posPrefixObj + 1);
		} else {
			var objectString = triple.object.slice(posQuotation + 1);
			posQuotation = objectString.indexOf("\"");
			objectString = objectString.slice(0, posQuotation);
			return objectString;
		}
	} else {
		return triple.object.slice(1, posTypeObj);
	}
}

function isEmpty(obj) {
	for ( var prop in obj) {
		if (obj.hasOwnProperty(prop))
			return false;
	}

	return true;
}

function guiRemoveResourceInstance(instanceName) {
	// Remove from list
	$("#" + instanceName).remove();
}

function guiRefreshProperty(instanceName, property, value) {
	$(".input" + instanceName).each(function(i, obj) {
		if ($(this).attr("name") === property) {
			$(this).val(value);
		}
	});
}

function guiRefreshLabels() {
	document.title = adapterName + " (" + adapterType + ")";
	$("h1").html(adapterName + " <small>" + adapterType + "</small>");
	$("#currentAdapterBreadcrumb").show();
	$("#currentAdapterName").html(adapterName);
	
}

function guiRefreshAdaptersList() {

$("#resourceInstancesBox").html("");

	for ( var index = 0; index < adapterInstances.length; ++index) {
		var object = adapterInstances[index];
		var text = '<div class="resourceHeader"><h2>' + object.name + '</h2>' + '<h3>Type: ' + object.type + '</h3>';
		text += '<div class="resourceButtons">';
		text += '<a href="?adaptername=' + object.name + '">';
		text += 'Manage Adapter</a> | ';
		text += ' <a href="http://localhost:8080/native/gui/lodlive/index.html?' + object.type.replace("#", "%23") + '">';
		text += 'Open visualization</a>';
		
		
		text += '</form></div><div class="clear"></div></div><div class="resourceProperties">';
		text += "Adapter Type: " + object.type + "</div>";

		d = document.createElement('div');
		$(d).addClass("resourceInstanceDiv").attr('id', object.name).html(text).appendTo($("#resourceInstancesBox")); // main
	}
}


function guiRefreshList() {
	
	$("#resourceInstancesBox").html("");

	for ( var index = 0; index < resourceInstances.length; ++index) {
		var object = resourceInstances[index];
		var text = '<div class="resourceHeader"><h2>' + object.name + '</h2>' + '<h3>Type: ' + object.type + '</h3>';
		text += '<div class="resourceButtons">';
		text += '<input class="resourceButton" type="button" id="buttonCancel' + object.name + '" value="Cancel" style="display: none;" onClick="cancelConfigure(\'' + object.name + '\');">';
		text += '<input class="resourceButton" type="button" id="buttonApply' + object.name + '" value="Apply" style="display: none;" onClick="applyConfigure(\'' + object.name + '\');">';
		text += '<input class="resourceButton" type="button" id="buttonConfigure' + object.name + '" value="Configure" onClick="configureResourceInstance(\'' + object.name + '\');">';
		text += '<input class="resourceButton" type="button" id="buttonRelease' + object.name + '" value="Release" onClick="releaseResourceInstance(\'' + object.name + '\');">';
		text += '</div><div class="clear"></div></div><div class="resourceProperties">';

		for ( var property in object) {
			if (!(property === "type") && !(property === "name") && !(property === "name_full")) {
				if (object.hasOwnProperty(property)) {
					text += '<div class="resourceProperty">';
					text += '<span class="propertyName">' + property + '</span><br/>';
					text += '<input type="text" size="' + Math.max(property.length, object[property].length) + '" name="' + property + '" class="propertyValue input' + object["name"] + '" value="'
							+ object[property] + '" readonly></div>';
				}
			}
		}
		text += "</div>";

		d = document.createElement('div');
		$(d).addClass("resourceInstanceDiv").attr('id', object.name).html(text).appendTo($("#resourceInstancesBox")); // main
	}
}

function guiAddCreateBox(adapterParameters) {
	
	$("#createResourceInstancesBox").html("");

	var text = '<div class="resourceHeader"><h2>Create Resource Instance</h2>' + '<h3>Type: ' + adapterResourceName + '</h3>';
	text += '<div class="resourceButtons">';
	text += '<input class="resourceButton" type="button" id="buttonCreate" value="Create" onClick="createNewResInstance();">';
	text += '</div><div class="clear"></div></div><div class="resourceProperties">';
	text += '<div class="resourceProperty">';
	text += '<span class="propertyName">name</span> (string)<br/>';
	text += '<input type="text" size="20" name="name" class="propertyValue inputCreate" id="inputCreateName"></div>';

	for ( var property in adapterParameters) {
		if (adapterParameters.hasOwnProperty(property)) {
			text += '<div class="resourceProperty">';
			text += '<span class="propertyName">' + property + '</span> (' + (adapterParameters[property]) + ')<br/>';
			text += '<input type="text" size="' + (property.length + adapterParameters[property].length) + '" name="' + property + '" class="propertyValue inputCreate"></div>';
		}
	}
	text += "</div>";

	d = document.createElement('div');
	$(d).addClass("resourceInstanceDiv").attr('id', 'createResourceInstanceBox').html(text).appendTo($("#createResourceInstancesBox")); // main
}

function releaseResourceInstance(instanceName) {
	restReleaseInstance(instanceName);
}

function configureResourceInstance(instanceName) {
	storeResInstanceBeforeConfigure(instanceName);
	toggleConfigureEditable(instanceName, true);
	toggleConfigureButtons(instanceName, true);
}

function storeResInstanceBeforeConfigure(instanceName) {
	for ( var index = 0; index < resourceInstances.length; ++index) {
		var object = resourceInstances[index];
		if (object.name === instanceName) {

			resourceInstanceBeforeConfigure = {};

			for ( var property in object) {
				if (object.hasOwnProperty(property)) {
					resourceInstanceBeforeConfigure[property] = object[property];
				}
			}

			break;
		}
	}

}

function applyConfigure(instanceName) {

	var configureTTL = getConfigureTTL(instanceName);
	console.log(configureTTL);
	restConfigureInstances(configureTTL);

	toggleConfigureButtons(instanceName, false);
	toggleConfigureEditable(instanceName, false);
}

function cancelConfigure(instanceName) {
	restoreResInstanceAfterCancel(instanceName);
	toggleConfigureButtons(instanceName, false);
	toggleConfigureEditable(instanceName, false);
}

function restoreResInstanceAfterCancel(instanceName) {

	$(".input" + instanceName).each(function(i, obj) {
		$(this).val(resourceInstanceBeforeConfigure[$(this).attr("name")]);
	});

}

function getConfigureTTL(instanceName) {
	var configureTTL = getPrefix() + getAdapterInstance();

	configureTTL += ":" + instanceName + " rdf:type <" + adapterResourceName + ">";

	$(".input" + instanceName).each(function(i, obj) {
		if (resourceInstanceBeforeConfigure[$(this).attr("name")] !== $(this).val()) {
			configureTTL += ";\n";
			configureTTL += adapterOntologyPrefix + ":" + $(this).attr("name") + " \"" + $(this).val() + "\"";
		}
	});
	configureTTL += ".\n";
	return configureTTL;
}

function getCreateTTL(instanceName) {
	var ttl = getPrefix() + getAdapterInstance();

	ttl += ":" + $("#inputCreateName").val() + " rdf:type <" + adapterResourceName + ">";

	$(".inputCreate").each(function(i, obj) {
		if ($(this).attr("name") !== "name") {
			ttl += ";\n";
			ttl += adapterOntologyPrefix + ":" + $(this).attr("name") + " \"" + $(this).val() + "\"";
		}
	});
	ttl += ".\n";
	return ttl;
}

function getPrefix() {
	var ttl = "@prefix fiteagle: <http://fiteagle.org/ontology#> .\n";
	ttl += "@prefix " + adapterOntologyPrefix + ": <" + adapterOntology + "> .\n";
	ttl += "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n";
	ttl += "@prefix : <http://fiteagleinternal#> .\n\n";
	return ttl;
}

function getAdapterInstance(){
	var ttl = ":" + adapterName + " rdf:type <" + adapterType + "> .\n";
	return ttl;
}

function createNewResInstance() {
	var createTTL = getCreateTTL();
	console.log(createTTL);
	restCreateInstance(createTTL);
}

function toggleConfigureEditable(instanceName, state) {
	if (state) {
		$(".input" + instanceName).prop('readonly', false);
		$(".input" + instanceName).css({
			"backgroundColor" : "white",
			"border" : "1px solid #000000"
		});
	} else {
		$(".input" + instanceName).prop('readonly', true);
		$(".input" + instanceName).css({
			"backgroundColor" : "#f9f9f9",
			"border" : "0"
		});
	}
}

function toggleConfigureButtons(instanceName, state) {
	if (state) {
		$("#buttonCancel" + instanceName).show();
		$("#buttonApply" + instanceName).show();
		$("#buttonConfigure" + instanceName).hide();
	} else {
		$("#buttonCancel" + instanceName).hide();
		$("#buttonApply" + instanceName).hide();
		$("#buttonConfigure" + instanceName).show();
	}
}
