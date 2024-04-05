/*
 * Copyright (c) 2010, 2024 BSI Business Systems Integration AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
import {LookupRow, objects, ProposalFieldEventMap, ProposalFieldModel, SmartField, SmartFieldLookupResult, strings} from '../../../index';
import $ from 'jquery';

export class ProposalField extends SmartField<string> implements ProposalFieldModel {
  declare model: ProposalFieldModel;
  declare eventMap: ProposalFieldEventMap;
  declare self: ProposalField;

  trimText: boolean;

  /**
   * If this flag is set to true the proposal field performs a lookup by text when
   * accept proposal is called. The behavior is similar to what the smart-field does
   * in that case, but without the need to have a valid single match as the result
   * from the lookup.
   */
  lookupOnAcceptByText: boolean;

  constructor() {
    super();

    this.maxLength = 4000;
    this.trimText = true;

    this.lookupOnAcceptByText = false;
  }

  protected override _getValueFromLookupRow(lookupRow: LookupRow<string>): string {
    return lookupRow.text;
  }

  protected override _getLastSearchText(): string {
    return this.value;
  }

  override cssClassName(): string {
    return 'proposal-field';
  }

  protected override _handleEnterKey(event: JQuery.KeyDownEvent) {
    // The state of 'this.popup' is different on various browsers. On some browsers (IE11) we don't
    // do CSS animations. This means IE11 sets the popup to null immediately whereas other browsers
    // use a timeout. Anyway: in case the popup is open at the time the user presses enter, we must
    // stop propagation (e.g. to avoid calls of other registered enter key-shortcuts, like the default
    // button on a form). See Widget.js for details about removing with or without CSS animations.
    let hasPopup = !!this.popup;
    this.acceptInput();
    if (this.popup) {
      this.closePopup();
    }
    if (hasPopup) {
      event.stopPropagation();
    }
  }

  protected override _lookupByTextOrAllDone(result: SmartFieldLookupResult<string>) {
    if (super._handleException(result)) {
      return;
    }
    if (result.lookupRows.length === 0) {
      this.setLoading(false);
      this._handleEmptyResult();
      return;
    }
    super._lookupByTextOrAllDone(result);
  }

  protected override _formatValue(value: string): string {
    if (objects.isNullOrUndefined(value)) {
      return '';
    }

    if (this.lookupRow) {
      return this._formatLookupRow(this.lookupRow);
    }

    return value;
  }

  protected override _validateValue(value: string): string {
    if (objects.isNullOrUndefined(value)) {
      return value;
    }
    let validValue = strings.asString(value);
    if (this.trimText) {
      validValue = validValue.trim();
    }
    if (validValue === '') {
      validValue = null;
    }
    return validValue;
  }

  protected override _ensureValue(value: string): string {
    return strings.asString(value);
  }

  /**
   * When 'clear' has been clicked (searchText is empty), we want to call customTextAccepted,
   * so the new value is sent to the server #221199.
   */
  protected override _acceptByText(sync: boolean, searchText: string) {
    $.log.isDebugEnabled() && $.log.debug('(ProposalField#_acceptByText) searchText=', searchText);
    let async = !sync;

    // In case sync=true we cannot wait for the results of the lookup-call,
    // that's why we simply accept the text that's already in the field
    if (async && this.lookupOnAcceptByText && strings.hasText(searchText)) {
      super._acceptByTextAsync(searchText);
    } else {
      this._customTextAccepted(searchText);
    }
  }

  /**
   * Only used in case lookupOnAcceptByText is true. It's basically the same code
   * as in the smart-field but without the error handling.
   */
  protected override _acceptByTextDone(result: SmartFieldLookupResult<string>) {
    this._userWasTyping = false;
    this._extendResult(result);

    // when there's exactly one result, we accept that lookup row
    if (result.uniqueMatch) {
      let lookupRow = result.uniqueMatch;
      if (this._isLookupRowActive(lookupRow)) {
        this.setLookupRow(lookupRow);
        this._inputAccepted();
        return;
      }
    }

    this._customTextAccepted(result.text);
  }

  protected override _checkResetLookupRow(value: string): boolean {
    return this.lookupRow && this.lookupRow.text !== value;
  }

  protected override _checkSearchTextChanged(searchText: string): boolean {
    return this._checkDisplayTextChanged(searchText);
  }

  protected _customTextAccepted(searchText: string) {
    this._setLookupRow(null); // only reset property lookup
    this._setValue(searchText);
    this._inputAccepted(true, false);
  }

  override getValueForSelection(): string {
    return this._showSelection() ? this.lookupRow.key : null;
  }

  /**
   * In ProposalField value and display-text is the same. When a custom text has been entered,
   * the value is set and the lookup-row is null.
   */
  protected override _copyValuesFromField(otherField: ProposalField) {
    if (this.lookupRow !== otherField.lookupRow) {
      this._setLookupRow(otherField.lookupRow); // only set property lookup
    }
    this.setErrorStatus(otherField.errorStatus);
    this.setDisplayText(otherField.displayText);
    this.setValue(otherField.value);
  }

  protected override _acceptInput(sync: boolean, searchText: string, searchTextEmpty: boolean, searchTextChanged: boolean, selectedLookupRow: LookupRow<string>): JQuery.Promise<void> | void {
    if (this.touchMode) {
      $.log.isDebugEnabled() && $.log.debug('(ProposalField#_acceptInput) Always send acceptInput for touch field');
      this._inputAccepted(true, !!selectedLookupRow);
      return;
    }

    // 1. Do nothing when search text did not change and is equals to the text of the current lookup row
    if (!searchTextChanged && !selectedLookupRow && this.lookupRow && this.lookupRow.text === searchText) {
      $.log.isDebugEnabled() && $.log.debug('(ProposalField#_acceptInput) unchanged: text is equals. Close popup');
      this._inputAccepted(false);
      return;
    }

    // 2. proposal chooser is open -> use the selected row as value
    if (selectedLookupRow) {
      $.log.isDebugEnabled() && $.log.debug('(ProposalField#_acceptInput) lookup-row selected. Set lookup-row, close popup lookupRow=', selectedLookupRow.toString());
      this.clearErrorStatus();
      this.setLookupRow(selectedLookupRow);
      this._inputAccepted();
      return;
    }

    // 3. proposal chooser is not open -> try to accept the current display text
    // this causes a lookup which may fail and open a new proposal chooser (property
    // change for 'result').
    if (searchTextChanged) {
      this.clearErrorStatus();
      this._acceptByText(sync, searchText);
    } else if (!this._hasUiError()) {
      this._inputAccepted(false);
    } else {
      // even though there's nothing to do, someone could wait for our promise to be resolved
      this._acceptInputDeferred.resolve();
    }

    return this._acceptInputDeferred.promise();
  }

  setTrimText(trimText: boolean) {
    this.setProperty('trimText', trimText);
  }

  protected override _updateEmpty() {
    this.empty = strings.empty(this.value);
  }
}
