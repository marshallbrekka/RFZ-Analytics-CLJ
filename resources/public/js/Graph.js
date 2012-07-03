function run() {
    var theForm = new FormBuilder(function(data) {
	getData(data);
	console.log(data);
	$("#loader").show();
  }, formspec);
    $("#data-form").append(theForm.form);
}


function form(container, submitCallback, sets, offsets, renderModes) {
    var self = this;
    this.sets = sets;
    this.offsets = offsets;
    this.renderModes = renderModes;
    this.plotCount = 0;
    this.container = container;
    this.form = $('<form/>');
    this.addBtn = $('<a href="#add">Add Plot</a>');
    this.addBtn.click(function() {
	self.addPlotOptions();
    });
    this.submit = $('<input type="submit" value="Submit"/>');
    this.container.append(this.form);
    this.form.append(this.addBtn);
    this.form.append(this.submit);
    console.log(container);
    this.addPlotOptions();
    this.form.submit(function() {
	submitCallback(self._getData());
	return false;
    });
}

form.prototype._getData = function() {
    var data = {};
    this.form.find('input, select').each(function(index, elem) {
	elem = $(elem);
	data[elem.attr('name')] = elem.val();
    });
    return data;
}

form.prototype.addPlotOptions = function() {
    var index = this.plotCount++;
    var items = [
	{
	    name : "set",
	    options : this.sets},
	{
	    name : "offset",
	    options : this.offsets},
	{
	    name : "render",
	    options : this.renderModes}
    ];
    var container = $('<div class="plotOptions" id="options-' + index + '"></div>');
    var removeBtn = $('<a href="#remove" class="remove">Remove</a>');
    container.append(removeBtn);
    removeBtn.click(function() {
	$("#options-" + index).remove();
    });
    for (var i = 0; i < items.length; i++) {
	var input = this._makeSelectionList(index, items[i].name, items[i].options);
	if(items[i].name == "set") {
	    input = this._makeSetSelection(index, items[i].options, input);
	}
	container.append(input);
    }
    container.insertBefore(this.addBtn);
}


form.prototype._makeSetSelection = function(index, sets, elem) {
    var container = $('<div/>');
    this._changeSetSelectionOptions(index, sets, elem, container);
    var self = this;
    elem.change(function() {
	self._changeSetSelectionOptions(index, sets, elem, container);
    });
    return container;
}
    

form.prototype._changeSetSelectionOptions = function(index, sets, elem, container) {
    var val = elem.val();
    
    container.children().detach();
    container.append(elem);
    var theSet = null;
    for (var i = 0; i < sets.length; i++) {
	if (val == sets[i].value) {
	    theSet = sets[i];
	    break;
	}
    }

    if (null != theSet.options) {
	var temp = theSet.options;
	for (var key in temp) {
	    if (temp.hasOwnProperty(key)) {
		container.append('<label>' + key + '</label><input type="text" name="plots[' + index + '][set-options][' + key + ']" />');
	    }
	}
    }
}
   

form.prototype._makeSelectionList = function(index, name, options) {
    var select = $('<select type="select" name="plots[' + index + '][' + name + ']"></select>');
    for (var i = 0; i < options.length; i++) {
	select.append('<option value="' + options[i].value + '">' + options[i].label + '</option>');
    }
    return select;
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
	var temp = data[i];
	series.push({
            data : temp,
            pointStart: new Date(temp[0][0]),
            pointInterval: 24 * 3600,
            tooltip: {
                valueDecimals: 2
            }
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

