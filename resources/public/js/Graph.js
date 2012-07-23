// File-level comments are helpful for people ot know what to
// expect. It helps them understand the code at an application level
// scope.
// 
// In general, you shouldn't declare things in the top level namespace.
// And in future projects, you should probably use modules (eg: rquire.js) (no biggie here tho)

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
        url:location.origin + location.pathname + 'api?'});
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
	var plot = data[i];
	for(var b = 0; b < plot.batches.length; b++) {
	    var batch = plot.batches[b];
	    series.push({
		data : batch.timelines,
		pointStart: new Date(batch.timelines[0][0]),
		pointInterval: 24 * 3600,
		tooltip: {
		    valueDecimals: 2
		},
		    name : plot.info.set[0] + " " + data[i].info.set[1] + " : " + batch.info
	    });
	}
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

