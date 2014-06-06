// SCOUT GUI
// (c) Copyright 2013-2014, BSI Business Systems Integration AG

scout.TabBox = function() {
  scout.TabBox.parent.call(this);
};
scout.inherits(scout.TabBox, scout.ModelAdapter);

scout.TabBox.prototype.init = function(model, session) {
  scout.TabBox.parent.prototype.init.call(this, model, session);
  //create groupbox adapter for selected tab
  this.session.getOrCreateModelAdapter(model.selectedTab, this);
  this.groupBoxes = this.session.getOrCreateModelAdapters(model.groupBoxes, this);
};

scout.TabBox.prototype._render = function($parent) {
  this.$container = $parent.appendDiv(undefined, 'tab-box');

  var i, groupBox;
  for (i = 0; i < this.groupBoxes.length; i++) {
    this.groupBoxes[i].attach(this.$container);
  }
};

scout.TabBox.prototype.dispose = function() {
  scout.TabBox.parent.prototype.dispose.call(this);
  var i, groupBox;
  for (i = 0; i < this.groupBoxes.length; i++) {
    groupBox = this.session.getModelAdapter(this.groupBoxes[i].id);
    if (groupBox) {
      groupBox.dispose();
    }
  }
};
