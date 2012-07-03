
/**
 * creates the api object to retrieve data 
 * @param {string} config.url location of the database
 * 
 */
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
           console.log("there was an error \n" + t);
       }
    });
}
