/**
 * Created by vju on 9/12/14.
 */


/**
 * Created by vju on 9/12/14.
 */

var AdapterlistHandler = function(){
    this.queryEndpoint = "http://localhost:8080/native/api/res/query";

    this.init = function(){
        var getAdapterListQuery = "SELECT ?adapter WHERE {?adapter  <http://www.w3.org/2000/01/rdf-schema#subClassOf>  <http://fiteagle.org/ontology#Adapter>}";

        var uri = this.queryEndpoint + "?query=" + encodeURIComponent(getAdapterListQuery);
        $.get( uri, function( resultSet ) {

            // alert( "Load was performed." );

            var res = resultSet.results.bindings;
            for(var i in res){
                var adapterURI = res[i].adapter.value;

                var link = document.createElement("a");
                link.href = "#";
                link.innerHTML = adapterURI;
                link.className = "list-group-item";
                $("#adapterlist")[0].appendChild(link);

            }
        });

    }.bind(this)

}
var adapterlistHandler = null;
$(document).ready(function () {
    adapterlistHandler = new AdapterlistHandler()
    adapterlistHandler.init();
});