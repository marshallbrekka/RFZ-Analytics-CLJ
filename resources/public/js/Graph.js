
 
 
function getData() {
    var data = new api({
        url:'http://' + location.hostname + ':' + location.port + '/api'
    });
    data.get({}, function(data){
        
        console.log(data.length);
        console.log(data);
        /*data = data.sort(function(a,b){
            if(a[0] < b[0])
                return -1;
            if(a[0] > b[0])
                return 1;
            return 0
        });*/
        //console.log(data.splice(0,100));
        //data = processData(data);
		$('#loader').hide();
        createGraph('container', data);
                
    }); 
} 



function createGraph(container, data) {
    window.chart = new Highcharts.StockChart({
        chart : {
            renderTo : container
        },

        rangeSelector : {
            selected : 1
        },

        title : {
            text : 'Total User Balance'
        },

        series : [{

            data : data,
            pointStart: new Date(data[0][0]),
            pointInterval: 24 * 3600,
            tooltip: {
                valueDecimals: 2
            }
        }]
    });


}
   
        
function processData(data) {
    var newData = [];
    var temp;
    for(var i = 0; i < data.length;) {
        var sameKeys = [i];
        var key = data[i][1];
        while(++i < data.length && data[i][1] == key) {
            sameKeys.push(i);
        }
        temp = 0;
        for(var k = 0; k < sameKeys.length; k++) {
            temp += data[sameKeys[k]][2];
        }
        newData.push([key, temp / sameKeys.length]);
    } 
    return newData;
}
