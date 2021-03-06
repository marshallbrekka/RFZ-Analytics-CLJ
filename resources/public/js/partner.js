(function() {
function Partner(selector, globOptions) {
    var chart = null;
    var api = function(config) {
	this.config = config;
    }

    api.prototype.get = function(filters, cb) {
	this._request(filters, cb);
    }

    api.prototype._request = function(data, cb) {
	$.ajax({
	    url : this.config.url,
	    data : data,
	    dataType : 'json',
	    success : function(data) {
		cb(data);
	    },
	    error : function(d, t) {
		//console.log("there was an error \n" + t);
	    }
	});
    }

    var timer = null;
    function loading() {
	var elem = $(selector);
	var text = elem.text();
	var base = "Loading";
	var dots = text.substr(base.length,text.length);
	if(dots.length < 3) {
	    dots += ".";
	} else {
	    dots = "";
	}
	elem.text(base + dots);
	timer = setTimeout(loading, 800);
    }



    
    function getData(options) {
	$(selector).text("Loading");
	loading();
	var data = new api({
	    url : globOptions.url + 'api?'});
	data.get(options, function(data){
	    clearTimeout(timer);
	    $(selector).text("");	    
	    createGraph($(selector).get(0), data);
                
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
		tooltip: {
		    valueDecimals: 2
		}
	    });
	}
    }


	function formatter() {
	    var interval = this.axis.tickInterval;
	    var type;
	    var types = {
		month : ["Month", 2592000000],
		week : ["Week", 604800000],
	        day : ["Day", 34560000]
	    };

	    for(var t in types) {
	    	if(types.hasOwnProperty(t)) {
		    if(interval >= types[t][1]) {
			type = t;
			break;
		    } 
		}
	    }	
  
	    var unit = Math.floor(this.value / types[type][1]) + 1;
	    var word = types[type][0];
	    if(unit > 1) {
		word = word + "s";				    
	    } 
	    return unit + " " + word;
	}	
	var chart = null;
	
	chart = new Highcharts.StockChart({
	    chart : {
		renderTo : container,
		zoomType : "x"
	    },

	    rangeSelector : {
		enabled : false
	    },
	    scrollbar : {
		enabled : false
	    },
	    navigator : {
		xAxis : {
		    labels : {
			    formatter : function(){return ""}
		    }
		}
	    },
	    tooltip : {
		enabled : false
	    },
	    xAxis : {
		labels : {
		    formatter : formatter			
		},
		minRange : 2592000000
	    },
	    series : series
	});


    }

    getData(globOptions.params);

}

window.Partner = Partner;
})();



new Partner("#container", {
    url : "http://localhost:8888/",
    params : {
	"plots[0][set][set]":"/subset/partner-users",
	"plots[0][set][email]":"citi@readyforzero.com",
	"plots[0][offset][offset]":"date-joined",
	"plots[0][render][render]":"total",
	"plots[0][batch][batch]":"merged"
    }
});
	
