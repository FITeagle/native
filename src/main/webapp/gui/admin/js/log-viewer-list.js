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
    var messagePool = [];


    this.init = function init() {
        var wsUriLogger = getRootUri() + "/bus/api/logger";

        var table = $('#example').DataTable( {
            data: null,
            "order": [[ 2, "asc" ]],
            "pageLength": 50,
            columns: [
                { data: 'method_type' },
                {data: "JMSCorrelationID"},
                {data:"timeString"},
                {
                    "targets": -1,
                    "data": null,
                    "defaultContent": "<button type='button' class='btn btn-default' data-content='Test' data-original-title='RDF' > <span class='glyphicon glyphicon-star'></span> RDF </button>"
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

            ],
            "createdRow": function ( row, data, index ) {
               
                $(row).attr('data-content',  data.rdf.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, "<br />"));


                $(row).popover({placement:"bottom",
                    trigger:"hover",
                    template: '<div class="popover" style="height: 600px; width:600px; min-width: 600px;" role="tooltip"><div class="arrow"></div><h3 class="popover-title">RDF</h3><div class="popover-content" style="height: 600px; width:600px; min-width: 600px;"></div></div>',
                    html:true


                });
                $("button", row).on("click", function(e){
                    e.stopPropagation();
                   // var data = table.row( $(this).parents('tr') ).data();
                    _this.createModal(data);
                })
            }

        } );
        var _this = this;
      /*  $('#example tbody').on( 'click', 'button', function (e) {
            e.stopPropagation();
            var data = table.row( $(this).parents('tr') ).data();
            _this.createModal(data);
        } );*/
     /*  $('#example tbody').on( 'click', 'tr', function () {
           var data = table.row( $(this)).data();
        $(this).attr('data-content',  data.rdf.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, "<br />"));


        $(this).popover({placement:"top",
            trigger:"click",
            template: '<div class="popover"  role="tooltip"><div class="arrow"></div><h3 class="popover-title">RDF</h3><div class="popover-content" style="height: 300 px; overflow-y: scroll"></div></div>'
        });
           //this.click();





       } );*/




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
        output[0].innerHTML += "\n \n --------------------------------------------------------------- \n \n";

        var pre = document.createElement("p");
        pre.style.wordWrap = "break-word";
        pre.style.color = "white";

        pre.innerHTML = data.rdf.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, "<br />");

        modal.modal();
        output[0].appendChild(pre);
        output[0].scrollTop = output[0].scrollHeight;





    };

}


$(document).ready(function () {
    var logViewer = new LogViewer()
    logViewer.init();

});
