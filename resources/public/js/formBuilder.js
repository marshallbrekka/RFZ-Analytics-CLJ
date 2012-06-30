(function() {
    $.dform.addType("select", function(opt) {
	var select = $('<select>').dform('attr', opt, ["options"]);
	$.each(opt.options, function(index, params) {
    	    var label = params.label;
	    select.append($('<option>').dform('attr', params, ["label"]).html(label).val(params.value));
	});
	
	return select;
    });
