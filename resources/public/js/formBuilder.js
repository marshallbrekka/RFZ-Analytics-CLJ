function FormBuilder(submitCallback, spec) {
    var self = this;
    this.form = $('<form>');
    this.addBtn = $('<a href="#add">Add Plot</a>').click(function() {
	self._addSet(spec);
    });
    var submit = $('<input type="submit" value="Submit"/>');
    this.form.append(this.addBtn);
    this.form.append(submit);
    this.form.submit(function() {
	submitCallback(self._getData());
	return false;
    });

    this._removeClosure = function(obj) {
	self._removeCallback(obj);
    };

    this.sets = [];
    this.types = this._getFieldTypes({}, spec);
    this._addSet(spec);
}

FormBuilder.prototype._getFieldTypes = function(fields, spec) {
    for (var field in spec) {
	    //console.log(field);
	    if (typeof spec[field] == "object") {
	        //console.log("about to parse object");
	        fields = this._getFieldTypes(fields, spec[field]);
	    } else if (field == "name") {
	        fields[spec[field]] = spec.type;
	    }
    }
    return fields;    
}

FormBuilder.prototype._processValue = function(type, value) {
    if (type == "timestamp") {
	return new Date(value).getTime();
    }
    if (type == "number") {
	return parseInt(value);
    } 
    return value;
}

FormBuilder.prototype._getData = function() {
    var data = {};
    for (var i = 0; i < this.sets.length; i++) {
	var setVals = this.sets[i].getValues();
	for (var name in setVals) {
	    if (setVals.hasOwnProperty(name)) {
		data['plots[' + i + ']' + name] = this._processValue(this.types[name], setVals[name]);
	    }
	}
    }
    return data;
}

FormBuilder.prototype._addSet = function(spec) {
    var set = new FormBuilder.Set(this._removeClosure, spec);
    set.container.insertBefore(this.addBtn);
    this.sets.push(set);
}

FormBuilder.prototype._removeCallback = function(set) {
    var index = this.sets.indexOf(set);
    this.sets.splice(index, 1);
}

FormBuilder.Set = function(removeCallback, spec) {
    var self = this;
    this.container = $('<div>');
    this.container.append($('<a href="#remove" class="remove">Remove</a>').click(function() {
	    self.container.remove();	    
	    removeCallback(self);
    }));

    for(var i = 0; i < spec.length; i++) {
        if (spec[i].multiple !== undefined) {
            this.container.append(this.multiple(spec[i]));
        } else {
            this.container.dform(spec[i]);
        }
    }
}

FormBuilder.Set.prototype.getValues = function() {
    var self = this,
        vals = {},
        multiNames = {};
        var nsCount = 0;
        var ns = null;

    this.container.find('.multi-field').each(function() {
                 $(this).find(':input').each(function() {
            var namespace = this.name.substr(0, this.name.indexOf("]") + 1);
            if(ns != namespace) {
                ns = namespace;
                nsCount = 0;
            }
            multiNames[this.name] = true;
            vals[self.splitNameWithIndex(this.name, nsCount)] = $(this).val();
        });
        nsCount++;
    });

            
    this.container.find(':input').each(function() {
        if(multiNames[this.name] == undefined) {
        	vals[this.name] = $(this).val();
        }
    });
    return vals;
}

FormBuilder.Set.prototype.splitNameWithIndex = function(name, index) {
    var split = name.indexOf("]") + 1;
    return name.substr(0, split) + "[" + index + "]" + name.substr(split);
}


FormBuilder.Set.prototype.multiple = function(spec) {
    var subContainer = $('<div/>');
    var self = this;
    function removed() {
        var fields = subContainer.find('.form-remove-field');
        if(fields.length < 2) {
            return false;
        } else if (fields.length == 2) {
           fields.removeClass("active");
        }
        return true;
    }
    var add = $('<a href="#addfield">Add</a>').click(function() {
        subContainer.find('.form-remove-field').addClass('active');
        subContainer.append(self.multipleAdd(spec, removed));
        return false;
    });
    subContainer.append(add);
    subContainer.append(this.multipleAdd(spec, removed));
    delete spec.caption;
    return subContainer;
}

FormBuilder.Set.prototype.multipleAdd = function(spec, removeFn) {
    var container = $('<div class="multi-field"/>');
    var remove = $('<a href="#removefield" class="form-remove-field">Remove</a>').click(function() {
        var canRemove = removeFn();
        if (canRemove) {
            container.remove();
        }
        return false;
    });
    container.append(remove);
    container.dform(spec);
    return container;
}


/**
 * builds a selection list with associated form fields for each option that are only visible when said option is selected
 * @param {string} options.type set to select-sub-fields
 * @param {object} options.options the list options, {key=value : value=label}
 * @param {object} options.fields the fields for each option {key=optionValue : value=(array of objects using the field format)}
 * @param {string} options.name the name for the selection list
 * @param {string} options.label the label for the select list
 */ 
$.dform.addType("select-sub-fields", function (options) {
    var container = $('<div>');
    var select = container.dform({type : "select", options :  options.options, name:options.name}).find("select");
    select.change(function() {
	var value = select.val();
	showOptions(value);
    });

    showOptions(Object.keys(options.options)[0]);

    function showOptions(val) {
	console.log(val);
	fields = options.fields[val];
	container.children().detach();
	container.append(select);
	for (var i = 0; i < fields.length; i++) {
	    container.dform(fields[i]);
	}
    }
    return container;
});


/**
 * creates form field with date picker attached
 * @param {string} options.type set to timestamp
 * @param {string} options.name form field name
 */
$.dform.addType("timestamp", function(options ){
    var input = $('<input type="text"/>').dform('attr', options);
    input.datepicker({minDate : new Date(1970, 0, 1)});
    return input;
});


/**
 * Builds a slider with min and max values. The param values are the same for min as they are for max
 * @param {string} options.type set this to range
 * @param {string} options.min.name the form field name for min
 * @param {int} options.min.value the min value
 */
$.dform.addType("range", function(options) {
    var container = $('<div>');
    var min = options.min, max = options.max;
    min.type = "hidden";
    max.type = "hidden";
    container.dform(min);
    container.dform(max);
    var slider = container.slider({
	min : min.value, 
	max : max.value, 
	values : [min.value, max.value],
	range : true,
	change : function(e, ui) {
	   container.find('[name="' + min.name + '"]').val(ui.values[0]);
	   container.find('[name="' + max.name + '"]').val(ui.values[1]);
	}
    });
    return container;
});
