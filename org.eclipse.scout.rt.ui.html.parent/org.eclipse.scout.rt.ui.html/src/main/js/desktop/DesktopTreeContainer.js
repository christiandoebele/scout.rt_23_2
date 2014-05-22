scout.DesktopTreeContainer = function($parent, model, session) {
  this.session = session;
  this._$desktopTree = $parent.appendDiv('DesktopTree');
  this.$div = this._$desktopTree;
  this.desktopTree = new scout.DesktopTree(model, session);
  this.desktopTree.attach(this._$desktopTree);
  this._addVerticalSplitter(this._$desktopTree);
};

scout.DesktopTreeContainer.prototype.onOutlineCreated = function(event) {
  if (this.desktopTree) {
    this.desktopTree.detach();
  }
  this.desktopTree = new scout.DesktopTree(event, this.session);
  this.desktopTree.attach(this._$desktopTree);
  this.desktopTree.attachModel();
};

scout.DesktopTreeContainer.prototype.onOutlineChanged = function(outlineId) {
  this.desktopTree.detach();
  this.desktopTree = this.session.widgetMap[outlineId];
  this.desktopTree.attach(this._$desktopTree);
  this.desktopTree.attachModel();
};

scout.DesktopTreeContainer.prototype.attachModel = function() {
  //TODO attachModel only necessary because setNodeSelection relies on desktop bench which is created later, is there a better way?
  this.desktopTree.attachModel();
};

scout.DesktopTreeContainer.prototype._addVerticalSplitter = function($div) {
  $div.appendDiv(undefined, 'splitter-vertical')
    .on('mousedown', '', resize);

  function resize() {
    var w;

    $('body').addClass('col-resize')
      .on('mousemove', '', resizeMove)
      .one('mouseup', '', resizeEnd);

    function resizeMove(event) {
      w = event.pageX + 11;

      $div.width(w);
      $div.next().width('calc(100% - ' + (w + 80) + 'px)')
        .css('left', w);

      if (w <= 180 && $div.attr('id') === 'DesktopTree') {
        $div.addClass('bread-crum');
      } else {
        $div.removeClass('bread-crum');
      }

    }

    function resizeEnd() {
      $('body').off('mousemove')
        .removeClass('col-resize');

      if (w < 180 && $div.attr('id') === 'DesktopTree') {
        w = 180;
        $div.animateAVCSD('width', w, null,
            function(i){$div.next().css('width', 'calc(100% - ' + (i + 80) + 'px)'); });
        $div.next().animateAVCSD('left', w);
      }
    }
    return false;
  }
};

