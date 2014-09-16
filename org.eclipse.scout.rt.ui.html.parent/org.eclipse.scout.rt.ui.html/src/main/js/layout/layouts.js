/**
 * This file contains various constant, classes and functions used for layouting.
 */
scout.LayoutConstants = {
    'MIN':0,
    'PREF':1,
    'MAX':2,
    'EPS':1e-6
};

/**
 * Abstract layout class with functions used by all layout algorithms.
 */
scout.AbstractLayout = function() {
  this.valid = false;
  this.validityBasedOnParentSize = new scout.Dimension();
};

scout.AbstractLayout.prototype._verifyLayout = function($parent) {
  var htmlParent = scout.HtmlComponent.get($parent);
  var parentSize = htmlParent.getSize();
  if (!this.valid || !this.validityBasedOnParentSize.equals(parentSize)) {
    this.validityBasedOnParentSize = parentSize;
    this.validateLayout($parent);
    this.valid = true;
  }
};

scout.AbstractLayout.prototype.invalidate = function() {
  this.valid = false;
};

/**
 * Form layout.
 */
scout.FormLayout = function() {
  scout.FormLayout.parent.call(this);
};
scout.inherits(scout.FormLayout, scout.AbstractLayout);

scout.FormLayout.prototype.layout = function($container) {
  var $rootGroupBox = $container.children('.root-group-box');
  var htmlRootGroupBox = scout.HtmlComponent.get($rootGroupBox);
  var htmlContainer = scout.HtmlComponent.get($container);
  // TODO AWE: (layout) add insets to root group box?
  htmlRootGroupBox.setSize(htmlContainer.getSize());
};

scout.FormLayout.prototype.preferredLayoutSize = function($container) {
  var $rootGroupBox = $container.children('.root-group-box');
  var htmlRootGroupBox = scout.HtmlComponent.get($rootGroupBox);
  return htmlRootGroupBox.getPreferredSize();
};

/**
 * Group-Box layout.
 */
scout.GroupBoxLayout = function() {
  scout.GroupBoxLayout.parent.call(this);
};
scout.inherits(scout.GroupBoxLayout, scout.AbstractLayout);

scout.GroupBoxLayout.prototype.layout = function($container) {
  var htmlContainer = scout.HtmlComponent.get($container);
  var containerSize = htmlContainer.getSize();
  var titleHeight = 28; // TODO: dynamisch ermitteln / visibility / existenz pruefen
  var bodyHeight = containerSize.height - titleHeight;
  var htmlBody = this._getHtmlBody($container);
  htmlBody.setSize(new scout.Dimension(containerSize.width, bodyHeight));
};

scout.GroupBoxLayout.prototype.preferredLayoutSize = function($container) {
  var htmlBody = this._getHtmlBody($container);
  var bodySize = htmlBody.getPreferredSize();
  var size = new scout.Dimension(bodySize.width, bodySize.height);
  size.height += 28;
  // TODO AWE: (layout) add insets to GroupBoxLayout
  return size;
};

scout.GroupBoxLayout.prototype._getHtmlBody = function($container) {
  var $body = $container.find('.group-box-body').first();
  return scout.HtmlComponent.get($body);
};

/**
 * Form-Field Layout, for a form-field with label, status-label and a field-
 */
scout.FormFieldLayout = function() {
  scout.FormFieldLayout.parent.call(this);
};
scout.inherits(scout.FormFieldLayout, scout.AbstractLayout);

scout.FormFieldLayout.prototype.layout = function($container) {
  var htmlComp = scout.HtmlComponent.get($container);
  var containerSize = htmlComp.getSize();
  var widthDiff = 0;
  var $label = $container.children('label');
  if ($label.isVisible()) {
    $label.css('width', '130px');
    widthDiff += 130;
  }
  var $status = $container.children('.status');
  if ($status.isVisible()) {
    $status.css('width', '10px');
    widthDiff += 10;
  }
  var $field = $container.children('.field');
  // TODO AWE: (layout) können wir hier nicht einfach setSize verwenden?
  $field.css('width', (containerSize.width - widthDiff) + 'px');
  $field.css('height', containerSize.height + 'px');
  // TODO AWE: (layout) dafür sorgen, dass wir hier immer ein get() machen können
  var htmlField = scout.HtmlComponent.optGet($field);
  if (htmlField) {
    htmlField.layout();
  }
};

scout.FormFieldLayout.prototype.preferredLayoutSize = function($container) {
  var width = 0,
    height = 23,
    $label = $container.children('label'),
    $status = $container.children('.status'),
    $field = $container.children('.field');
  if ($label.isVisible()) {
    width += 130;
  }
  if ($status.isVisible()) {
    width += 10;
  }
  if ($field.isVisible()) {
    // TODO AWE: (layout) dafür sorgen, dass wir hier immer ein get() machen können
    var prefSize, htmlField = scout.HtmlComponent.optGet($field);
    if (htmlField) {
      prefSize = htmlField.getPreferredSize();
    } else {
      prefSize = new scout.Dimension($field.width(), $field.height());
    }
    width += prefSize.width;
    height += prefSize.height;
  }
  return new scout.Dimension(width, height);
};


/**
 * Text-Field Layout, used to calculate the preferred size of a HTML text-field. Note that this is not the same as the
 * "auto" size of the HTML element. Browsers typically render a text-field larger than the minimum size to display the whole text.
 */
scout.TextFieldLayout = function() {
  scout.TextFieldLayout.parent.call(this);
};
scout.inherits(scout.TextFieldLayout, scout.AbstractLayout);

scout.TextFieldLayout.prototype.preferredLayoutSize = function($parent) {
   return scout.graphics.measureString($parent.val());
};

/**
 * Button Field Layout, for fields with a button.
 */
scout.ButtonFieldLayout = function() {
  scout.ButtonFieldLayout.parent.call(this);
};
scout.inherits(scout.ButtonFieldLayout, scout.AbstractLayout);

scout.ButtonFieldLayout.prototype.layout = function($parent) {
  // button has no children - nothing to do here
};

scout.ButtonFieldLayout.prototype.preferredLayoutSize = function($parent) {
  var $button = $parent.find('button');
  var hMargin = $button.outerWidth(true) - $button.width();
  var vMargin = $button.outerHeight(true) - $button.height();
  var textSize = scout.graphics.measureString($button.html());
  return new scout.Dimension(textSize.width + hMargin, textSize.height + vMargin);
};

