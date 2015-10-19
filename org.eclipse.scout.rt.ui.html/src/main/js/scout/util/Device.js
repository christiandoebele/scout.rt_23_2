/* global FastClick */
/**
 * Provides information about the device and its supported features.<p>
 * The informations are detected lazily.
 */
scout.Device = function(userAgent) {
  this.userAgent = userAgent;
  this.system;
  this.features = {};
  this.type = scout.Device.Type.DESKTOP;
  this.browser = scout.Device.SupportedBrowsers.UNKNOWN;
  this.browserVersion = 0;

  // --- device specific configuration
  // initialize with empty string so that it can be used without calling initUnselectableAttribute()
  this.unselectableAttribute = '';
  this.tableAdditionalDivRequired = false;
  this.focusManagerActive = true;

  this.parseUserAgent(userAgent);
  this.parseBrowserVersion(userAgent);
};

scout.Device.vendorPrefixes = ['Webkit', 'Moz', 'O', 'ms', 'Khtml'];

scout.Device.SupportedBrowsers = {
  UNKNOWN: 'Unknown',
  FIREFOX: 'Firefox',
  CHROME: 'Chrome',
  INTERNET_EXPLORER: 'InternetExplorer',
  SAFARI: 'Safari'
};

scout.Device.System = {
  IOS: 'IOS',
  ANDROID: 'ANDROID'
};

scout.Device.Type = {
  DESKTOP: 'DESKTOP',
  TABLET: 'TABLET',
  MOBILE: 'MOBILE'
};

/**
 * Called during bootstrap by index.html before the session startup.<p>
 * Precalculates the value of some attributes to store them
 * in a static way (and prevent many repeating function calls within loops).<p>
 * Also loads device specific scripts (fast click for ios devices)
 */
scout.Device.prototype.bootstrap = function() {
  var deferreds = [];

  // Precalculate value and store in a simple property, to prevent many function calls inside loops (e.g. when generating table rows)
  this.unselectableAttribute = this.getUnselectableAttribute();
  this.tableAdditionalDivRequired = this.isTableAdditionalDivRequired();

  if (this.isIos()) {
    // We use Fastclick to prevent the 300ms delay when touching an element.
    // With Chrome 32 the issue is solved, so no need to load the script for other devices than iOS
    deferreds.push(this._loadScriptDeferred('res/fastclickmod-1.0.1.min.js', function() {
        FastClick.attach(document.body);
        $.log.info('FastClick script loaded and attached');
      }));
  }
  if (this.hasOnScreenKeyboard()) {
    // Auto focusing of elements is bad with on screen keyboards -> deactivate to prevent unwanted popping up of the keyboard
    this.focusManagerActive = false;

    deferreds.push(this._loadScriptDeferred('res/jquery.mobile.custom-1.4.5.min.js', function() {
        $.log.info('JQuery Mobile script loaded');
      }));
  }
  return deferreds;
};

scout.Device.prototype._loadScriptDeferred = function(scriptUrl, doneFunc) {
  return $
    .getCachedScript(scriptUrl)
    .done(doneFunc);
};

// FIXME AWE/CGU: find a better way to check for on-screen keyboard
// must also work for Windows Surface devices. There's an ungly solution
// described here: http://stackoverflow.com/questions/26531016/detect-windows-8-on-screen-keyboard-with-javascript
// Some forum post suggest that it's not possible to detect a virtual keyboard
// properly and that the user should decide whether or not the application should run
// in a virtual keyboard mode, or not.
scout.Device.prototype.hasOnScreenKeyboard = function() {
  return this.isIos() || this.isAndroid();
};

scout.Device.prototype.isIos = function() {
  return this.system === scout.Device.System.IOS;
};

scout.Device.prototype.isAndroid = function() {
  return this.system === scout.Device.System.ANDROID;
};

/**
 * This method returns false for very old browsers. Basically we check for the first version
 * that supports ECMAScript 5. This methods excludes all browsers that are known to be
 * unsupported, all others (e.g. unknown engines) are allowed by default.
 */
scout.Device.prototype.isSupportedBrowser = function(browser, version) {
  browser = scout.helpers.nvl(browser, this.browser);
  version = scout.helpers.nvl(version, this.browserVersion);
  var browsers = scout.Device.SupportedBrowsers;
  if ((browser === browsers.INTERNET_EXPLORER && version < 9) ||
      (browser === browsers.CHROME && version < 23) ||
      (browser === browsers.FIREFOX && version < 21) ||
      (browser === browsers.SAFARI && version < 7)) {
    return false;
  }
  return true;
};

scout.Device.prototype.parseUserAgent = function(userAgent) {
  if (!userAgent) {
    return;
  }
  this._parseSystem(userAgent);
  this._parseBrowser(userAgent);
};

scout.Device.prototype._parseSystem = function(userAgent) {
  var i, device;

  if (userAgent.indexOf('iPhone') > -1) {
    this.system = scout.Device.System.IOS;
    this.type = scout.Device.Type.MOBILE;
  } else if (userAgent.indexOf('iPad') > -1) {
    this.system = scout.Device.System.IOS;
    this.type = scout.Device.Type.TABLET;
  } else if (userAgent.indexOf('Android') > -1) {
    this.system = scout.Device.System.ANDROID;
    if (userAgent.indexOf('Mobile') > -1) {
      this.type = scout.Device.Type.MOBILE;
    } else {
      this.type = scout.Device.Type.TABLET;
    }
  }
};

scout.Device.prototype._parseBrowser = function(userAgent) {
  if (userAgent.indexOf('Firefox')  > -1) {
    this.browser = scout.Device.SupportedBrowsers.FIREFOX;
  } else if (userAgent.indexOf('MSIE') > -1 || userAgent.indexOf('Trident') > -1) {
    this.browser = scout.Device.SupportedBrowsers.INTERNET_EXPLORER;
  } else if (userAgent.indexOf('Chrome') > -1) {
    this.browser = scout.Device.SupportedBrowsers.CHROME;
  } else if (userAgent.indexOf('Safari') > -1) {
    this.browser = scout.Device.SupportedBrowsers.SAFARI;
  }
};

scout.Device.prototype.supportsFeature = function(property, checkFunc) {
  if (this.features[property] === undefined) {
    this.features[property] = checkFunc(property);
  }
  return this.features[property];
};

scout.Device.prototype.supportsTouch = function() {
  // Implement when needed, see https://hacks.mozilla.org/2013/04/detecting-touch-its-the-why-not-the-how/
  return this.hasOnScreenKeyboard();
};

scout.Device.prototype.supportsFile = function() {
  return (window.File ? true : false);
};

scout.Device.prototype.supportsCssAnimation = function() {
  return this.supportsCssProperty('animation');
};

scout.Device.prototype.supportsCssUserSelect = function() {
  return this.supportsCssProperty('userSelect');
};

scout.Device.prototype.supportsInternationalization = function() {
  return window.Intl && typeof window.Intl === 'object';
};

/**
 * Returns true if the device supports the download of resources in the same window as the single page app is running.
 * With "download" we mean: change <code>window.location.href</code> to the URL of the resource to download. Some browsers don't
 * support this behavior and require the resource to be opened in a new window with <code>window.open</code>.
 */
scout.Device.prototype.supportsDownloadInSameWindow = function() {
  return scout.Device.SupportedBrowsers.FIREFOX !== this.browser;
};

scout.Device.prototype.hasPrettyScrollbars = function() {
  return this.supportsFeature('_prettyScrollbars', check.bind(this));

  function check(property) {
    var SYSTEM = scout.Device.System;
    // TODO CGU add windows phone, what about desktop windows with touch support? Maybe add touch support to scrollbars?
    return SYSTEM.IOS === this.system ||
    SYSTEM.ANDROID === this.system;
  }
};

scout.Device.prototype.supportsCopyFromDisabledInputFields = function() {
  return scout.Device.SupportedBrowsers.FIREFOX !== this.browser;
};

scout.Device.prototype.supportsCssProperty = function(property) {
  return this.supportsFeature(property, check);

  function check(property) {
    var i;
    if (document.body.style[property] !== undefined) {
      return true;
    }

    property = property.charAt(0).toUpperCase() + property.slice(1);
    for (i = 0; i < scout.Device.vendorPrefixes.length; i++) {
      if (document.body.style[scout.Device.vendorPrefixes[i] + property] !== undefined) {
        return true;
      }
    }

    return false;
  }
};

/**
 * Returns '' for modern browsers, that support the 'user-select' CSS property.
 * Returns ' unselectable="on"' for IE9.
 * This string can be used to add to any HTML element as attribute.
 */
scout.Device.prototype.getUnselectableAttribute = function() {
  return this.supportsFeature('_unselectableAttribute', function(property) {
    if (this.supportsCssUserSelect()) {
      return '';
    }
    // workaround for IE 9
    return ' unselectable="on"';
  }.bind(this));
};

/**
 * Returns false for modern browsers, that support CSS table-cell properties restricted
 * with a max-width and hidden overflow. Returns true if an additional div level is required.
 */
scout.Device.prototype.isTableAdditionalDivRequired = function() {
  return this.supportsFeature('_tableAdditionalDivRequired',  function(property) {
    var test = $('body').appendDiv();
    test.text('Scout');
    test.css('visibility', 'hidden');
    test.css('display', 'table-cell');
    test.css('max-width', '1px');
    test.css('overflow', 'hidden');
    var result = test.width() > 1;
    test.remove();
    return result;
  }.bind(this));
};

scout.Device.prototype.supportsIframeSecurityAttribute = function() {
  return this.supportsFeature('_iframeSecurityAttribute', function(property) {
    var test = document.createElement('iframe');
    return ('security' in test);
  }.bind(this));
};

/**
 * Currently the browserVersion is only set for IE. Because the only version-check we do,
 * is whether or not we use an old IE version. Version regex only matches the first number pair
 * but not the revision-version. Example:
 * - 21     match: 21
 * - 21.1   match: 21.1
 * - 21.1.3 match: 21.1
 *
 */
scout.Device.prototype.parseBrowserVersion = function(userAgent) {
  var versionRegex, browsers = scout.Device.SupportedBrowsers;
  if (this.browser === browsers.INTERNET_EXPLORER) {
    // with internet explorer 11 user agent string does not contain the 'MSIE' string anymore
    // additionally in new version the version-number after Trident/ is not the browser-version
    // but the engine-version.
    if (userAgent.indexOf('MSIE') > -1) {
      versionRegex = /MSIE ([0-9]+\.?[0-9]*)/;
    } else {
      versionRegex = /rv:([0-9]+\.?[0-9]*)/;
    }
  } else if (this.browser === browsers.SAFARI) {
    versionRegex = /Version\/([0-9]+\.?[0-9]*)/;
  } else if (this.browser === browsers.FIREFOX) {
    versionRegex = /Firefox\/([0-9]+\.?[0-9]*)/;
  } else if (this.browser === browsers.CHROME) {
    versionRegex = /Chrome\/([0-9]+\.?[0-9]*)/;
  }
  if (versionRegex) {
    var matches = versionRegex.exec(userAgent);
    if (Array.isArray(matches) && matches.length === 2) {
      this.browserVersion = parseFloat(matches[1]);
    }
  }
};

// ------------ Singleton ----------------

scout.device = new scout.Device(navigator.userAgent);

//XXX AWE: do not check in
scout.device.system = scout.Device.System.IOS;
scout.device.type = scout.Device.Type.TABLET;
scout.device.focusManagerActive = false;
