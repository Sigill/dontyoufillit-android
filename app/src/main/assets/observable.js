function Observable() {
	this.observers = [];
}
Observable.prototype.addObserver = function(observer) {
	this.observers.push(observer);
};

Observable.prototype.removeObserver = function(observer) {
	var index = this.observers.indexOf(observer);

	if(~index) {
		this.observers.splice(index, 1);
	}
};

Observable.prototype.hasObserver = function(observer) {
	var index = this.observers.indexOf(observer);

	if(~index) {
		return true;
	} else {
		return false;
	}
};

Observable.prototype.notifyObservers = function() {
	for(var i = this.observers.length - 1; i >= 0; i--) {
		this.observers[i].apply(null, arguments);
	};
};
