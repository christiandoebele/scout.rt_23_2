package org.eclipse.scout.rt.ui.html;

import java.util.Arrays;
import java.util.Set;

public class UiTextContributor implements IUiTextContributor {

  @Override
  public void contributeUiTextKeys(Set<String> textKeys) {
    textKeys.addAll(Arrays.asList(
        // From org.eclipse.scout.rt.client
        "Remove",
        "ResetTableColumns",
        "ColumnSorting",
        "Column",
        "Cancel",
        "Ok",
        // From org.eclipse.scout.rt.ui.html
        "ui.CalendarToday",
        "ui.CalendarDay",
        "ui.CalendarWork",
        "ui.CalendarWeek",
        "ui.CalendarCalendarWeek",
        "ui.CalendarMonth",
        "ui.CalendarYear",
        "ui.InvalidDateFormat",
        "ui.EmptyCell",
        "ui.FilterBy_",
        "ui.SearchFor_",
        "ui.TableRowCount0",
        "ui.TableRowCount1",
        "ui.TableRowCount",
        "ui.NumRowsSelected",
        "ui.NumRowsFiltered",
        "ui.NumRowsFilteredBy",
        "ui.RemoveFilter",
        "ui.NumRowsLoaded",
        "ui.ReloadData",
        "ui.Reload",
        "ui.showEveryDate",
        "ui.groupedByWeekday",
        "ui.groupedByMonth",
        "ui.groupedByYear",
        "ui.otherValues",
        "ui.Count",
        "ui.ConnectionInterrupted",
        "ui.ConnectionReestablished",
        "ui.Reconnecting_",
        "ui.SelectAll",
        "ui.SelectNone",
        "ui.ServerError",
        "ui.SessionTimeout",
        "ui.SessionExpiredMsg",
        "ui.Move",
        "ui.toBegin",
        "ui.forward",
        "ui.backward",
        "ui.toEnd",
        "ui.ascending",
        "ui.descending",
        "ui.ascendingAdditionally",
        "ui.descendingAdditionally",
        "ui.Sum",
        "ui.overEverything",
        "ui.overSelection",
        "ui.grouped",
        "ui.ColorCells",
        "ui.fromRedToGreen",
        "ui.fromGreenToRed",
        "ui.withBarGraph",
        "ui.remove",
        "ui.add",
        "ui.FilterBy",
        "ui.Show",
        "ui.Up",
        "ui.Back",
        "ui.Continue",
        "ui.Ignore",
        "ui.ErrorCodeX",
        "ui.InternalUiErrorMsg",
        "ui.UiInconsistentMsg",
        "ui.UnexpectedProblem",
        "ui.InternalProcessingErrorMsg",
        "ui.PleaseWait_",
        "ui.ShowAllNodes",
        "ui.CW",
        "ui.ChooseFile",
        "ui.ChooseFiles",
        "ui.Upload",
        "ui.Browse",
        "ui.FromXToY",
        "ui.To",
        "ui.FileSizeLimitTitle",
        "ui.FileSizeLimit",
        "ui.ClipboardTimeoutTitle",
        "ui.ClipboardTimeout",
        "ui.PopupBlockerDetected",
        "ui.OpenManually",
        "ui.FileChooserHint",
        "ui.Outlines"
        ));
  }
}
