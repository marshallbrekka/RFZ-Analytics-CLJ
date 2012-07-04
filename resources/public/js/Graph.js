function run() {
    var theForm = new FormBuilder(function(data) {
	getData(data);
	console.log(data);
	$("#loader").show();
  }, formspec);
    $("#data-form").append(theForm.form);
}

   
 
function getData(options) {
    var data = new api({
        url:'http://' + location.hostname + ':' + location.port + '/api?'});
    data.get(options, function(data){
        
        console.log(data.length);
        console.log(data);
	$('#loader').hide();
        createGraph('container', data);
                
    }); 
} 
    

function createGraph(container, data) {
    var series = [];
    for (var i = 0; i < data.length; i++) {
	var temp = data[i].points;
	series.push({
            data : temp,
            pointStart: new Date(temp[0][0]),
            pointInterval: 24 * 3600,
            tooltip: {
                valueDecimals: 2
            },
	    name : data[i].info.set[0] + " " + data[i].info.set[1]
        });
    }

    window.chart = new Highcharts.StockChart({
        chart : {
            renderTo : container
        },

        rangeSelector : {
            selected : 1
        },

        title : {
            text : 'User Balance Graphs'
        },

        series : series
    });


}

