function run() {
    

  $("#data-form").submit(function() {
    var idSet = $('#user-set').val();
    var type = $('input:radio[name=render_type]:checked').val();
    var offset = $("#offset").val();
    getData({"id-set" : idSet, render_type : type, offset : offset});
    $("#loader").show();
    return false;
  });
}


function form(submitCallback, sets, offsets, renderModes) {
    this.sets = sets;
    this.offsets = offsets;
    this.renderModes = renderModes;
    this.container = $('<div class="options"/>');
    this.form = $('<form/>');
    this.addBtn = $('<a href="#add">Add Plot</a>');
    this.submit = $('<input type="submit" value="Submit"/>');
    this.form.append(addBtn);
    this.form.append(submit);
    this.addPlotOptions();
    this.form.submit(function() {
	submitCallback(this._getData());
	return false;
    }
}

form._getData = function() {
    var data = {};
    form.find('input, select').each(function(index, elem) {
	data[elem.getAttribute('name')] = elem.val();
    }
    return data;
}

form.addPlotOptions = function() {
    var index = plotOptions.length;
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
    }
    for (var i = 0; i < items.length; i++) {
	var input = this._makeSelectionList(items[i].name, items[i].options);
	container.append(input);
    }
    container.insertBefore(this.addBtn);
}	
    

form._makeSelectionList = function(index, name, options) {
    var select = $('<select type="select" name="plots[' + index + '][' + name + ']"></select>');
    for (var i = 0; i < options; i++) {
	select.append('<option value="' + options[i].value + '">' + options[i].label + '</option>');
    }
    return select;
}


 
function getData(options) {
    var data = new api({
        url:'http://' + location.hostname + ':' + location.port + '/api?render_type=average'    });
    data.get(options, function(data){
        
        console.log(data.length);
        console.log(data);
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
   
