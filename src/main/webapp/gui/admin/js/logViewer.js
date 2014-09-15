/**
 * Created by vju on 03.09.14.
 */



function getRootUri() {
    return "ws://" + (document.location.hostname == "" ? "localhost" : document.location.hostname) + ":" + (document.location.port == "" ? "8080" : document.location.port);
}
var LogViewer = function () {
    this.ws = null;//websocket
    var requestPool = [];
    var responsePool = [];
    var messagePool = []

    this.init = function init() {
        var wsUriLogger = getRootUri() + "/bus/api/logger";

        var table = $('#example').DataTable( {
            data: null,
            columns: [
                { data: 'method_type' },
                {data: "JMSCorrelationID"},
                {data:"timeString"},
                {
                    "targets": -1,
                    "data": null,
                    "defaultContent": "<button type='button' class='btn btn-default'> <span class='glyphicon glyphicon-star'></span> RDF </button>"
                },
                {
                    data:"rdf",
                    "visible": false
                },
                {   data: "timeInt",
                    "visible": false

                }

            ],
            "columnDefs": [
                { "orderData": [ 5 ],    "targets": 2 }

            ]
        } );
        var _this = this;
        $('#example tbody').on( 'click', 'button', function () {
            var data = table.row( $(this).parents('tr') ).data();
            _this.createModal(data);
        } );



        this.ws = new WebSocket(wsUriLogger);
        this.ws.onmessage = function (evt) {
            this.addMessageToPool($.parseJSON(evt.data));
        }.bind(this);
    }.bind(this);
    this.addMessageToPool = function (jsonMessage) {
        if (jsonMessage.hasOwnProperty("method_type")) {
            var date = new Date();
            var methodType = jsonMessage.method_type;
            jsonMessage.timeInt = date.getTime();
            jsonMessage.timeString = date.toLocaleTimeString();
            jsonMessage.responseList = [];
            messagePool.push(jsonMessage);
        }
        else if (jsonMessage.hasOwnProperty("response")) {
            var date = new Date();
            var methodType = jsonMessage.response;
            var pos = messagePool.push(jsonMessage) - 1;
            jsonMessage.method_type = methodType;
            jsonMessage.timeInt = date.getTime();
            jsonMessage.timeString = date.toLocaleTimeString();
            if (jsonMessage.hasOwnProperty("JMSCorrelationID")) {
                for (var i = 0; i < messagePool.length - 1; i++) {
                    var request = messagePool[i];
                    if (jsonMessage.JMSCorrelationID == request.JMSCorrelationID) {
                        request.responseList.push(pos);
                    }
                }
            }
        }
        if(!jsonMessage.hasOwnProperty("JMSCorrelationID")){
            jsonMessage.JMSCorrelationID = "N.A.";
        }
        if(!jsonMessage.hasOwnProperty("rdf")){
            jsonMessage.rdf = "N.A.";
        }
        var table = $('#example').DataTable();
        table.row.add(jsonMessage).draw();
    }.bind(this);

    this.createModal = function(data){
        var modal = $("#detailModal");
        var output = $("#modalOutput");
        output[0].innerHTML = "";

        var pre = document.createElement("p");
        pre.style.wordWrap = "break-word";
        pre.style.color = "white";

        pre.innerHTML = data.rdf.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, "<br />");





        output[0].appendChild(pre);
        modal.modal();
    };

}


$(document).ready(function () {
    var logViewer = new LogViewer()
    logViewer.init();

});
