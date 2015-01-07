
function getRootUri() {
    return "ws://" + (document.location.hostname == "" ? "localhost" : document.location.hostname) + ":" + (document.location.port == "" ? "8080" : document.location.port);
}
var LogViewer = function () {
    this.ws = null;
    var requestPool = [];
    var responsePool = [];
    var messagePool = [];


    this.init = function init() {
        var wsUriLogger = getRootUri() + "/bus/api/logger";

        var table = $('#example').DataTable( {
            data: null,
            "order": [[ 3, "asc" ]],
            "pageLength": 50,
            columns: [
                { data: 'METHOD_TYPE' },
                { data: 'METHOD_TARGET' },
                {data: "JMSCorrelationID"},
                {data:"timeString"},
                {
                    "targets": -1,
                    "data": null,
                    "defaultContent": "<button type='button' class='btn btn-default' data-content='Test' data-original-title='body' > <span class='glyphicon glyphicon-star'></span> Body </button>"
                },
                {
                    data:"body",
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
               
                $(row).attr('data-content',  data.body.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, "<br />"));


                $(row).popover({placement:"bottom",
                    trigger:"hover",
                    template: '<div class="popover" style="height: 600px; width:600px; min-width: 600px;" role="tooltip"><div class="arrow"></div><h3 class="popover-title">Body</h3><div class="popover-content" style="height: 600px; width:600px; min-width: 600px;"></div></div>',
                    html:true


                });
                $("button", row).on("click", function(e){
                    e.stopPropagation();
                    _this.createModal(data);
                })
            }

        } );
        var _this = this;

        this.ws = new WebSocket(wsUriLogger);
        this.ws.onmessage = function (evt) {
            this.addMessageToPool($.parseJSON(evt.data));
        }.bind(this);
    }.bind(this);
    this.addMessageToPool = function (jsonMessage) {
        if (jsonMessage.hasOwnProperty("METHOD_TYPE")) {
            var date = new Date();
            var methodType = jsonMessage.method_type;
            jsonMessage.timeInt = date.getTime();
            jsonMessage.timeString = date.toLocaleTimeString();
            jsonMessage.responseList = [];
            messagePool.push(jsonMessage);
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

        pre.innerHTML = data.body.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, "<br />");

        modal.modal();
        output[0].appendChild(pre);
        output[0].scrollTop = output[0].scrollHeight;
    };

}


$(document).ready(function () {
    var logViewer = new LogViewer()
    logViewer.init();

});
