/*******************************************************************************
 * Copyright (c) 2013 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.commons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.awt.Color;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.scout.commons.HTMLUtility.DefaultFont;
import org.junit.Test;

/**
 * JUnit for {@link HTMLUtility}
 */
public class HTMLUtilityTest {

  @Test
  public void testStyleHtmlTextNullAndEmptyValues() throws Exception {
    // null values
    String output = "" +
        "<html>" +
        "<head>" +
        "<meta http-equiv=\"content-type\" content=\"text/html;charset=UTF-8\"/>" +
        "</head>" +
        "<body style=\"overflow:auto;\">" +
        "</body>" +
        "</html>";
    assertEquals(output, HTMLUtility.cleanupHtml(null, true, false, null));

    // empty html
    output = "" +
        "<html>" +
        "<head>" +
        "<meta http-equiv=\"content-type\" content=\"text/html;charset=UTF-8\"/>" +
        "</head>" +
        "<body style=\"overflow:auto;\">" +
        "</body>" +
        "</html>";
    assertEquals(output, HTMLUtility.cleanupHtml("", true, false, null));
  }

  @Test
  public void testStyleHtmlTextCompleteHtml() throws Exception {
    String input = "" +
        "<html>" +
        "<body>" +
        "Test content" +
        "</body>" +
        "</html>";
    String output = "" +
        "<html>" +
        "<head>" +
        "<meta http-equiv=\"content-type\" content=\"text/html;charset=UTF-8\"/>" +
        "</head>" +
        "<body style=\"overflow:auto;\">" +
        "Test content" +
        "</body>" +
        "</html>";

    // without encoding information
    assertEquals(output, HTMLUtility.cleanupHtml(input, true, false, null));

    // with encoding information
    input = "" +
        "<html>" +
        "<head>" +
        "<meta http-equiv=\"content-type\" content=\"text/html;charset=UTF-8\"/>" +
        "</head>" +
        "<body>" +
        "Test content" +
        "</body>" +
        "</html>";
    assertEquals("HTML with encoding information", output, HTMLUtility.cleanupHtml(input, true, false, null));

    input = "" +
        "<html>" +
        "<head>" +
        "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\"/>" +
        "</head>" +
        "<body>" +
        "Test content" +
        "</body>" +
        "</html>";
    assertEquals("different encoding than UTF-8", output, HTMLUtility.cleanupHtml(input, true, false, null));

    input = "" +
        "<html>" +
        "<head>" +
        "<meta  http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\"/>" +
        "</head>" +
        "<body>" +
        "Test content" +
        "</body>" +
        "</html>";
    assertEquals("different encoding with additional space", output, HTMLUtility.cleanupHtml(input, true, false, null));

    input = "" +
        "<html>" +
        "<head>" +
        "<meta \n http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\"/>" +
        "</head>" +
        "<body>" +
        "Test content" +
        "</body>" +
        "</html>";
    assertEquals("different encoding with additional new line", output, HTMLUtility.cleanupHtml(input, true, false, null));

    output = "" +
        "<html>" +
        "<head>" +
        "<meta http-equiv=\"content-type\" content=\"text/html;charset=UTF-8\"/>" +
        "<meta http-equiv=\"description\" content=\"This page provides test content\"/>" +
        "</head>" +
        "<body style=\"overflow:auto;\">" +
        "Test content" +
        "</body>" +
        "</html>";
    input = "" +
        "<html>" +
        "<head>" +
        "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\"/>" +
        "<meta http-equiv=\"description\" content=\"This page provides test content\"/>" +
        "</head>" +
        "<body>" +
        "Test content" +
        "</body>" +
        "</html>";
    assertEquals("complete html document with multiple meta tags (including Content-Type)", output, HTMLUtility.cleanupHtml(input, true, false, null));

    output = "" +
        "<html>" +
        "<head>" +
        "<meta http-equiv=\"description\" content=\"This page provides test content\"/>" +
        "<meta http-equiv=\"content-type\" content=\"text/html;charset=UTF-8\"/>" +
        "</head>" +
        "<body style=\"overflow:auto;\">" +
        "Test content" +
        "</body>" +
        "</html>";
    input = "" +
        "<html>" +
        "<head>" +
        "<meta http-equiv=\"description\" content=\"This page provides test content\"/>" +
        "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\"/>" +
        "</head>" +
        "<body>" +
        "Test content" +
        "</body>" +
        "</html>";
    assertEquals("complete html document with multiple meta tags in different order (including Content-Type)", output, HTMLUtility.cleanupHtml(input, true, false, null));

    output = "" +
        "<html>" +
        "<head>" +
        "<meta http-equiv=\"content-type\" content=\"text/html;charset=UTF-8\"/>" +
        "<meta http-equiv=\"description\" content=\"This page provides test content\"/>" +
        "</head>" +
        "<body style=\"overflow:auto;\">" +
        "Test content" +
        "</body>" +
        "</html>";
    input = "" +
        "<html>" +
        "<head>" +
        "<meta http-equiv=\"description\" content=\"This page provides test content\"/>" +
        "</head>" +
        "<body>" +
        "Test content" +
        "</body>" +
        "</html>";
    assertEquals("complete html document with meta tag different than Content-Type", output, HTMLUtility.cleanupHtml(input, true, false, null));
  }

  @Test
  public void testStyleHtmlDefaultFont() throws Exception {
    String output = "" +
        "<html>" +
        "<head>" +
        "<meta http-equiv=\"content-type\" content=\"text/html;charset=UTF-8\"/>" +
        "<style type=\"text/css\">" +
        "body{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "table{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "tr{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "th{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "td{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "span{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "</style>" +
        "</head>" +
        "<body style=\"overflow:auto;\">" +
        "Test content" +
        "</body>" +
        "</html>";
    String input = "" +
        "<html>" +
        "<body>" +
        "Test content" +
        "</body>" +
        "</html>";
    DefaultFont defaultFont = createDefaultFont(10, 0xA0FF00, "Arial");
    assertEquals(output, HTMLUtility.cleanupHtml(input, true, false, defaultFont));

    output = "" +
        "<html>" +
        "<head>" +
        "<meta http-equiv=\"content-type\" content=\"text/html;charset=UTF-8\"/>" +
        "<style type=\"text/css\">" +
        "body{font-family:Arial,'Times New Roman',sans-serif;font-size:10pt;color:#a0ff00;}" +
        "table{font-family:Arial,'Times New Roman',sans-serif;font-size:10pt;color:#a0ff00;}" +
        "tr{font-family:Arial,'Times New Roman',sans-serif;font-size:10pt;color:#a0ff00;}" +
        "th{font-family:Arial,'Times New Roman',sans-serif;font-size:10pt;color:#a0ff00;}" +
        "td{font-family:Arial,'Times New Roman',sans-serif;font-size:10pt;color:#a0ff00;}" +
        "span{font-family:Arial,'Times New Roman',sans-serif;font-size:10pt;color:#a0ff00;}" +
        "</style>" +
        "</head>" +
        "<body style=\"overflow:auto;\">" +
        "Test content" +
        "</body>" +
        "</html>";
    input = "" +
        "<html>" +
        "<body>" +
        "Test content" +
        "</body>" +
        "</html>";
    defaultFont = createDefaultFont(10, 0xA0FF00, "Arial", "Times New Roman", "sans-serif");
    assertEquals(output, HTMLUtility.cleanupHtml(input, true, false, defaultFont));

    output = "" +
        "<html>" +
        "<head>" +
        "<meta http-equiv=\"content-type\" content=\"text/html;charset=UTF-8\"/>" +
        "<style type=\"text/css\">" +
        "body{font-family:Arial,'Times New Roman',sans-serif;font-size:10pt;color:#a0ff00;}" +
        "table{font-family:Arial,'Times New Roman',sans-serif;font-size:10pt;color:#a0ff00;}" +
        "tr{font-family:Arial,'Times New Roman',sans-serif;font-size:10pt;color:#a0ff00;}" +
        "th{font-family:Arial,'Times New Roman',sans-serif;font-size:10pt;color:#a0ff00;}" +
        "td{font-family:Arial,'Times New Roman',sans-serif;font-size:10pt;color:#a0ff00;}" +
        "span{font-family:Arial,'Times New Roman',sans-serif;font-size:10pt;color:#a0ff00;}" +
        "</style>" +
        "</head>" +
        "<body style=\"overflow:auto;\">" +
        "Test content" +
        "</body>" +
        "</html>";
    input = "" +
        "<html>" +
        "<head>" +
        "<style>" +
        "</style>" +
        "</head>" +
        "<body>" +
        "Test content" +
        "</body>" +
        "</html>";
    defaultFont = createDefaultFont(10, 0xA0FF00, "Arial", "Times New Roman", "sans-serif");
    assertEquals(output, HTMLUtility.cleanupHtml(input, true, false, defaultFont));

    output = "" +
        "<html>" +
        "<head>" +
        "<meta http-equiv=\"content-type\" content=\"text/html;charset=UTF-8\"/>" +
        "<style type=\"text/css\">" +
        "body { font:bold .9em Times; }" +
        "table{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "tr{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "th{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "td{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "span{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "</style>" +
        "</head>" +
        "<body style=\"overflow:auto;\">" +
        "Test content" +
        "</body>" +
        "</html>";
    input = "" +
        "<html>" +
        "<head>" +
        "<style>" +
        "body { font:bold .9em Times; }" +
        "</style>" +
        "</head>" +
        "<body>" +
        "Test content" +
        "</body>" +
        "</html>";
    defaultFont = createDefaultFont(10, 0xA0FF00, "Arial");
    assertEquals(output, HTMLUtility.cleanupHtml(input, true, false, defaultFont));

    output = "" +
        "<html>" +
        "<head>" +
        "<meta http-equiv=\"content-type\" content=\"text/html;charset=UTF-8\"/>" +
        "<style type=\"text/css\">" +
        "body { font:bold .9em Times; }" +
        "td { font:italic 1cm Helvetica; }" +
        "table{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "tr{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "th{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "span{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "</style>" +
        "</head>" +
        "<body style=\"overflow:auto;\">" +
        "Test content" +
        "</body>" +
        "</html>";
    input = "" +
        "<html>" +
        "<head>" +
        "<style>" +
        "body { font:bold .9em Times; }" +
        "td { font:italic 1cm Helvetica; }" +
        "</style>" +
        "</head>" +
        "<body>" +
        "Test content" +
        "</body>" +
        "</html>";
    defaultFont = createDefaultFont(10, 0xA0FF00, "Arial");
    assertEquals(output, HTMLUtility.cleanupHtml(input, true, false, defaultFont));

    output = "" +
        "<html>" +
        "<head>" +
        "<meta http-equiv=\"content-type\" content=\"text/html;charset=UTF-8\"/>" +
        "<style type=\"text/css\">" +
        "td { font:italic 1cm Helvetica; }" +
        "body{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "table{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "tr{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "th{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "span{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "</style>" +
        "</head>" +
        "<body style=\"overflow:auto;\">" +
        "Test content" +
        "</body>" +
        "</html>";
    input = "" +
        "<html>" +
        "<head>" +
        "<style>" +
        "td { font:italic 1cm Helvetica; }" +
        "</style>" +
        "</head>" +
        "<body>" +
        "Test content" +
        "</body>" +
        "</html>";
    defaultFont = createDefaultFont(10, 0xA0FF00, "Arial");
    assertEquals(output, HTMLUtility.cleanupHtml(input, true, false, defaultFont));

    output = "" +
        "<html>" +
        "<head>" +
        "<meta http-equiv=\"content-type\" content=\"text/html;charset=UTF-8\"/>" +
        "<style type=\"text/css\">" +
        "td { font:italic 1cm Helvetica; }" +
        "body{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "table{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "tr{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "th{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "span{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "</style>" +
        "</head>" +
        "<body style=\"overflow:auto;\">" +
        "Test content" +
        "</body>" +
        "</html>";
    input = "" +
        "<html>" +
        "<head>" +
        "<style>" +
        "td { font:italic 1cm Helvetica; }" +
        "body{}" +
        "</style>" +
        "</head>" +
        "<body>" +
        "Test content" +
        "</body>" +
        "</html>";
    defaultFont = createDefaultFont(10, 0xA0FF00, "Arial");
    assertEquals(output, HTMLUtility.cleanupHtml(input, true, false, defaultFont));

    output = "" +
        "<html>" +
        "<head>" +
        "<meta http-equiv=\"content-type\" content=\"text/html;charset=UTF-8\"/>" +
        "<style type=\"text/css\">" +
        "td { font:italic 1cm Helvetica; }" +
        "body{font-family:sans-serif;font-size:10pt;color:#a0ff00;}" +
        "table{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "tr{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "th{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "span{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "</style>" +
        "</head>" +
        "<body style=\"overflow:auto;\">" +
        "Test content" +
        "</body>" +
        "</html>";
    input = "" +
        "<html>" +
        "<head>" +
        "<style>" +
        "td { font:italic 1cm Helvetica; }" +
        "body{font-family:sans-serif}" +
        "</style>" +
        "</head>" +
        "<body>" +
        "Test content" +
        "</body>" +
        "</html>";
    defaultFont = createDefaultFont(10, 0xA0FF00, "Arial");
    assertEquals(output, HTMLUtility.cleanupHtml(input, true, false, defaultFont));

    output = "" +
        "<html>" +
        "<head>" +
        "<meta http-equiv=\"content-type\" content=\"text/html;charset=UTF-8\"/>" +
        "<style type=\"text/css\">" +
        "td { font:italic 1cm Helvetica; }" +
        "body{font-family:sans-serif;font-size:20em;color:#a0ff00;}" +
        "table{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "tr{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "th{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "span{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "</style>" +
        "</head>" +
        "<body style=\"overflow:auto;\">" +
        "Test content" +
        "</body>" +
        "</html>";
    input = "" +
        "<html>" +
        "<head>" +
        "<style>" +
        "td { font:italic 1cm Helvetica; }" +
        "body{font-family:sans-serif;font-size:20em}" +
        "</style>" +
        "</head>" +
        "<body>" +
        "Test content" +
        "</body>" +
        "</html>";
    defaultFont = createDefaultFont(10, 0xA0FF00, "Arial");
    assertEquals(output, HTMLUtility.cleanupHtml(input, true, false, defaultFont));

    output = "" +
        "<html>" +
        "<head>" +
        "<meta http-equiv=\"content-type\" content=\"text/html;charset=UTF-8\"/>" +
        "<style type=\"text/css\">" +
        "td { font:italic 1cm Helvetica; }" +
        "body{font-family:sans-serif;font-size:20em;color:#FFFFFF;}" +
        "table{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "tr{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "th{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "span{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "</style>" +
        "</head>" +
        "<body style=\"overflow:auto;\">" +
        "Test content" +
        "</body>" +
        "</html>";
    input = "" +
        "<html>" +
        "<head>" +
        "<style>" +
        "td { font:italic 1cm Helvetica; }" +
        "body{font-family:sans-serif;font-size:20em;color:#FFFFFF;}" +
        "</style>" +
        "</head>" +
        "<body>" +
        "Test content" +
        "</body>" +
        "</html>";
    defaultFont = createDefaultFont(10, 0xA0FF00, "Arial");
    assertEquals(output, HTMLUtility.cleanupHtml(input, true, false, defaultFont));

    output = "" +
        "<html>" +
        "<head>" +
        "<meta http-equiv=\"content-type\" content=\"text/html;charset=UTF-8\"/>" +
        "<style type=\"text/css\">" +
        "td { font:italic 1cm Helvetica; }" +
        "body{font-family:sans-serif;font-size:10pt;color:#a0ff00;}" +
        "table{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "tr{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "th{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "span{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "</style>" +
        "</head>" +
        "<body style=\"overflow:auto;\">" +
        "Test content" +
        "</body>" +
        "</html>";
    input = "" +
        "<html>" +
        "<head>" +
        "<style>" +
        "td { font:italic 1cm Helvetica; }" +
        "body{font-family:sans-serif}" +
        "</style>" +
        "</head>" +
        "<body>" +
        "Test content" +
        "</body>" +
        "</html>";
    defaultFont = createDefaultFont(10, 0xA0FF00, "Arial");
    assertEquals(output, HTMLUtility.cleanupHtml(input, true, false, defaultFont));

    input = "" +
        "<html>" +
        "<head>" +
        "<style type=\"text/css\">" +
        "li {font-weight:bold;font-size:18;text-align:center;color:white;}\n" +
        "ul {font-weight:bold;font-size:18;text-align:center;color:white;}" +
        "body, td {font-family:arial;font-size:12;background-color:#EBF4F8;margin:0;}\n" +
        "a {color: #67A8CE;}\n" +
        "td {vertical-align: top;}\n" +
        "td.bullet {font-weight:bold;font-size:18;text-align:center;color:white;}" +
        "</style>" +
        "</head>" +
        "<body>" +
        "</body>" +
        "</html>";

    output = "" +
        "<html>" +
        "<head>" +
        "<style type=\"text/css\">" +
        "li {font-weight:bold;font-size:18;text-align:center;color:white;}\n" +
        "ul {font-weight:bold;font-size:18;text-align:center;color:white;}" +
        "body, td {font-family:arial;font-size:12;background-color:#EBF4F8;margin:0;color:#a0ff00;}\n" +
        "a {color: #67A8CE;}\n" +
        "td {vertical-align: top;}\n" +
        "td.bullet {font-weight:bold;font-size:18;text-align:center;color:white;font-family:Arial;}" +
        "table{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "tr{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "th{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "span{font-family:Arial;font-size:10pt;color:#a0ff00;}" +
        "</style>" +
        "</head>" +
        "<body style=\"overflow:auto;\">" +
        "</body>" +
        "</html>";

    defaultFont = createDefaultFont(10, 0xA0FF00, "Arial");
    assertEquals(output, HTMLUtility.cleanupHtml(input, false, false, defaultFont));
  }

  @Test
  public void testStyleHtmlDefaultHyperlinkColor() throws Exception {
    String input = "";
    String output = "";
    Color color = new Color(0xFF00FF);

    /*
     * no color defined should do no CSS changes and no formatting
     */
    input = "<a href=\"x\">link to x</a>";
    output = "" +
        "<html>" +
        "<head>" +
        "</head>" +
        "<body style=\"overflow:auto;\">" +
        "<a href=\"x\">link to x</a>" +
        "</body>" +
        "</html>" +
        "";
    assertEquals(output, HTMLUtility.cleanupHtml(input, false, false, null));
    assertEquals(output, HTMLUtility.cleanupHtml(input, false, false, null, null));

    /*
     * color != null should put this color in a CSS definition for hyperlinks
     * (if there is not such definition already) and format the HTML source
     */
    input = "<a href=\"x\">link to x</a>";
    output = "" +
        "<html>" +
        "<head>" +
        /* */"<style type=\"text/css\">" +
        /* */"a {color: " + ColorUtility.rgbToText(color.getRed(), color.getGreen(), color.getBlue()) + ";}\n" +
        /* */"</style>" +
        "</head>" +
        "<body style=\"overflow:auto;\">" +
        "<a href=\"x\">link to x</a>" +
        "</body>" +
        "</html>" +
        "";
    assertEquals(formatHtml(output), HTMLUtility.cleanupHtml(input, false, false, null, color));

    /*
     * color != null should NOT put this color in a CSS definition for hyperlinks
     * if there is such a definition already - but the HTML source will be formatted
     * because it has been transformed for the analysis
     */
    input = "" +
        "<html>" +
        "  <head>" +
        "    <style type=\"text/css\">" +
        "      a {color: #000000;}\n" +
        "    </style>" +
        "  </head>" +
        "  <body>" +
        "    <a href=\"x\">link to x</a>" +
        "  </body>" +
        "</html>" +
        "";
    output = "" +
        "<html>" +
        "<head>" +
        "<style type=\"text/css\">" +
        "a {color: #000000;}\n" +
        "</style>" +
        "</head>" +
        "<body style=\"overflow:auto;\">" +
        "<a href=\"x\">link to x</a>" +
        "</body>" +
        "</html>" +
        "";
    assertEquals(formatHtml(output), HTMLUtility.cleanupHtml(input, false, false, null, color));

  }

  private String formatHtml(String htmlText) {
    return HTMLUtility.toHtmlText(HTMLUtility.toHtmlDocument(htmlText));
  }

  @Test
  public void testHtmlEncodeAndDecode() throws Exception {
    String value = "a\nb\tc   d<br/><p>e</p>";
    String htmlEncoded = StringUtility.htmlEncode(value, true);

    assertEquals("a<br/>b<span style=\"white-space:pre\">&#9;</span>c&nbsp;&nbsp;&nbsp;d&lt;br/&gt;&lt;p&gt;e&lt;/p&gt;", StringUtility.htmlEncode(value, true));
    assertEquals("a\nb\tc   d\n<p>e</p>", StringUtility.htmlDecode(htmlEncoded));
  }

  private DefaultFont createDefaultFont(int size, int color, String... fontFamilies) {
    DefaultFont defaultFont = new DefaultFont();
    defaultFont.setSize(size);
    defaultFont.setForegroundColor(color);
    defaultFont.setSizeUnit("pt");
    defaultFont.setFamilies(fontFamilies);
    return defaultFont;
  }

  /**
   * Test cases for {@link HTMLUtility#toPlainTextWithTable(String)}.
   *
   * @see Bug 415316
   */
  @Test
  public void testToPlainTextWithTable() {
    testToPlainTextWithTableConversion("htmlExample.html");
    testToPlainTextWithTableConversion("htmlExample_headTagMissing.html");
    testToPlainTextWithTableConversion("htmlExample_bodyTagMissing.html");
    testToPlainTextWithTableConversion("htmlExample_bodyAndHeadTagMissing.html");
  }

  /**
   * @param fileName
   */
  private void testToPlainTextWithTableConversion(String fileName) {
    String htmlString = readFileFromClasspath(fileName);
    String plainExpected = readFileFromClasspath("htmlToPlain_expectedResult.txt").trim();
    String plaintext = HTMLUtility.toPlainTextWithTable(htmlString).trim();
    assertEquals(fileName + " was not converted to plaintext as expected.", plainExpected, plaintext);
  }

  /**
   * @param fileName
   * @return
   */
  private String readFileFromClasspath(String fileName) {
    String packageName = getClass().getPackage().getName();
    packageName = packageName.replaceAll("\\.", "/");
    fileName = StringUtility.join("/", packageName, "fixture", fileName);

    InputStream in = getClass().getClassLoader().getResourceAsStream(fileName);
    try {
      return new String(IOUtility.getContent(in), "UTF-8");
    }
    catch (Exception e) {
      fail("could not read input file: " + fileName);
      return null;
    }
  }

  /**
   * Test cases for {@link HTMLUtility#toPlainTextWithTable(String)} with comments.
   *
   * @See Bug 415316
   */
  @Test
  public void testToPlainTextWithTableWithComments() {
    String input;

    //HTML comments in the body
    input = "<html>" +
        "<head><title>Title</title></head>" +
        "<body>my <!-- comment --> body <!-- second comment --></body>" +
        "</html>";
    assertEquals("my body", HTMLUtility.toPlainTextWithTable(input));

    //HTML comments in the body (incl. the empty comment form) and in title
    input = "<html>" +
        "<head><title>Title <!-- comment --></title></head>" +
        "<body><!---->my body<!-- comment --></body>" +
        "</html>";
    assertEquals("my body", HTMLUtility.toPlainTextWithTable(input));

    //HTML comments in the body (incl. new lines) and in title
    input = "<html>" +
        "<head><title><!-- \n Title \n comment --> A title</title></head>" +
        "<body><!--\nfoo\nbar-->my <!-- \n x \n --> body<!-- comment --></body>" +
        "</html>";
    assertEquals("my body", HTMLUtility.toPlainTextWithTable(input));

    //The document contains a comment containing <body>..</body>. This comment needs to be skiped
    input = "<html>" +
        "<head>" +
        "<title>Title</title>" +
        "</head>" +
        "<!--<body>commented body</body> -->" +
        "<body>normal body</body>" +
        "</html>";
    assertEquals("normal body", HTMLUtility.toPlainTextWithTable(input));
  }

  @Test
  public void testToPlainTextWithTableNewLines() {
    String in1 = "<html>" +
        "<head><title>Title</title></head>" +
        "<body>Before<p />After</body>" +
        "</html>";

    String in2 = "<html>" +
        "<head><title>Title</title></head>" +
        "<body>Before<p class=\"test\"></p>After</body>" +
        "</html>";

    String in3 = "<html>" +
        "<head><title>Title</title></head>" +
        "<body>Before<p class=\"test\">InBetween</p>After</body>" +
        "</html>";

    String in4 = "<html>" +
        "<head><title>Title</title></head>" +
        "<body>Before<p class=\"test\">   \t\n</p>After</body>" +
        "</html>";

    String in5 = "<html>" +
        "<head><title>Title</title></head>" +
        "<body>Before<p class=\"test\">&nbsp;</p>After</body>" +
        "</html>";

    String in6 = "<html>" +
        "<head><title>Title</title></head>" +
        "<body>Before<p/>After</body>" +
        "</html>";

    String in7 = "<html>" +
        "<head><title>Title</title></head><body>" +
        "<p class=\"MsoNormal\"><span style=\"font-family:&quot;Arial&quot;,&quot;sans-serif&quot;;mso-fareast-language:FR-CH\">Sehr geehrte Damen und Herren<o:p></o:p></span></p>\n" +
        "<p class=\"MsoNormal\"><span style=\"font-family:&quot;Arial&quot;,&quot;sans-serif&quot;;mso-fareast-language:FR-CH\">Keine<o:p></o:p></span></p>\n" +
        "<p class=\"MsoNormal\"><span style=\"font-family:&quot;Arial&quot;,&quot;sans-serif&quot;;mso-fareast-language:FR-CH\"><o:p>&nbsp;</o:p></span></p>\n" +
        "<p class=\"MsoNormal\"><span style=\"font-family:&quot;Arial&quot;,&quot;sans-serif&quot;;mso-fareast-language:FR-CH\">Eine<o:p></o:p></span></p>" +
        "</body></html>";

    String in8 = "<html xmlns:v=\"urn:schemas-microsoft-com:vml\" xmlns:o=\"urn:schemas-microsoft-com:office:office\" xmlns:w=\"urn:schemas-microsoft-com:office:word\" xmlns:m=\"http://schemas.microsoft.com/office/2004/12/omml\" xmlns=\"http://www.w3.org/TR/REC-html40\">\n" +
        "<head>\n" +
        "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n" +
        "<meta name=\"Generator\" content=\"Microsoft Word 14 (filtered medium)\">\n" +
        "<!--[if gte mso 9]><xml>\n" +
        "<o:shapedefaults v:ext=\"edit\" spidmax=\"1026\" />\n" +
        "</xml><![endif]--><!--[if gte mso 9]><xml>\n" +
        "<o:shapelayout v:ext=\"edit\">\n" +
        "<o:idmap v:ext=\"edit\" data=\"1\" />\n" +
        "</o:shapelayout></xml><![endif]-->\n" +
        "</head>\n" +
        "<body lang=\"DE-CH\" link=\"blue\" vlink=\"purple\">\n" +
        "<div class=\"WordSection1\">\n" +
        "<p class=\"MsoNormal\"><span lang=\"FR-CH\" style=\"font-family:&quot;Arial&quot;,&quot;sans-serif&quot;;color:#1F497D\">Test<o:p></o:p></span></p>\n" +
        "<p class=\"MsoNormal\"><span lang=\"FR-CH\"><o:p>&nbsp;</o:p></span></p>\n" +
        "<div>\n" +
        "<div style=\"border:none;border-top:solid #B5C4DF 1.0pt;padding:3.0pt 0cm 0cm 0cm\">\n" +
        "<p class=\"MsoNormal\"><b><span lang=\"DE\" style=\"font-family:&quot;Tahoma&quot;,&quot;sans-serif&quot;;mso-fareast-language:DE-CH\">Von:</span></b><span lang=\"DE\" style=\"font-family:&quot;Tahoma&quot;,&quot;sans-serif&quot;;mso-fareast-language:DE-CH\"> Bla, Bla Inc\n\r" +
        "<br>\n\r" +
        "<b>Gesendet:</b> Mittwoch, 14. Oktober 2015 14:01<br>\n\r" +
        "<b>An:</b> Peter Muster, blub<br>\n\r" +
        "<b>Betreff:</b> Test Betreff<o:p></o:p></span></p>\n\r" +
        "</div>\n" +
        "</div>\n" +
        "<p class=\"MsoNormal\"><span lang=\"FR-CH\"><o:p>&nbsp;</o:p></span></p>\n" +
        "<p class=\"MsoNormal\"><span style=\"font-family:&quot;Arial&quot;,&quot;sans-serif&quot;\">-----Ursprüngliche Nachricht-----<o:p></o:p></span></p>\n" +
        "<p class=\"MsoNormal\"><span style=\"font-family:&quot;Arial&quot;,&quot;sans-serif&quot;\">Von: <a href=\"mailto:blablub.bla@hotmail.com\">\n" +
        "peter.muster@bla.ch</a><o:p></o:p></span></p>\n" +
        "<p class=\"MsoNormal\"><span style=\"font-family:&quot;Arial&quot;,&quot;sans-serif&quot;\">Gesendet: 14.10.15 13:52<o:p></o:p></span></p>\n" +
        "<p class=\"MsoNormal\"><span style=\"font-family:&quot;Arial&quot;,&quot;sans-serif&quot;\">An: blablub &lt;<a href=\"mailto:blablub.bla@hotmail.com\">blablub.bla@hotmail.com</a>&gt;<o:p></o:p></span></p>\n" +
        "<p class=\"MsoNormal\"><span style=\"font-family:&quot;Arial&quot;,&quot;sans-serif&quot;\">Betreff: Test E-Mail<o:p></o:p></span></p>\n" +
        "<p class=\"MsoNormal\"><span style=\"font-family:&quot;Arial&quot;,&quot;sans-serif&quot;\"><o:p>&nbsp;</o:p></span></p>\n" +
        "<p class=\"MsoNormal\"><span style=\"font-family:&quot;Arial&quot;,&quot;sans-serif&quot;\">Hallo<o:p></o:p></span></p>\n" +
        "<p class=\"MsoNormal\"><span style=\"font-family:&quot;Arial&quot;,&quot;sans-serif&quot;\"><o:p></o:p></span></p>\n" +
        "<p class=\"MsoNormal\"><span style=\"font-family:&quot;Arial&quot;,&quot;sans-serif&quot;\">Das ist ein Testmail<o:p></o:p></span></p>\n" +
        "<p class=\"MsoNormal\"><span style=\"font-family:&quot;Arial&quot;,&quot;sans-serif&quot;\">Keine Leerzeile vor dieser Zeile<o:p></o:p></span></p>\n" +
        "<p class=\"MsoNormal\"><span style=\"font-family:&quot;Arial&quot;,&quot;sans-serif&quot;\"><o:p></o:p></span></p>\n" +
        "<p class=\"MsoNormal\"><span style=\"font-family:&quot;Arial&quot;,&quot;sans-serif&quot;\">Vor dieser Zeile hats 1 Leerzeile<o:p></o:p></span></p>\n" +
        "<p class=\"MsoNormal\"><span style=\"font-family:&quot;Arial&quot;,&quot;sans-serif&quot;\"><o:p></o:p></span></p>\n" +
        "<p class=\"MsoNormal\"><span style=\"font-family:&quot;Arial&quot;,&quot;sans-serif&quot;\">&nbsp;<o:p></o:p></span></p>\n" +
        "<p class=\"MsoNormal\"><span style=\"font-family:&quot;Arial&quot;,&quot;sans-serif&quot;\">Davor 2<o:p></o:p></span></p>\n" +
        "<p class=\"MsoNormal\"><span style=\"font-family:&quot;Arial&quot;,&quot;sans-serif&quot;\"><o:p></o:p></span></p>\n" +
        "<p class=\"MsoNormal\"><span style=\"font-family:&quot;Arial&quot;,&quot;sans-serif&quot;\">&nbsp;<o:p></o:p></span></p>\n" +
        "<p class=\"MsoNormal\"><span style=\"font-family:&quot;Arial&quot;,&quot;sans-serif&quot;\">&nbsp;<o:p></o:p></span></p>\n" +
        "<p class=\"MsoNormal\"><span style=\"font-family:&quot;Arial&quot;,&quot;sans-serif&quot;\">Davor 3<o:p></o:p></span></p>\n" +
        "<p class=\"MsoNormal\"><span style=\"font-family:&quot;Arial&quot;,&quot;sans-serif&quot;\"><o:p></o:p></span></p>\n" +
        "<p class=\"MsoNormal\"><span style=\"font-family:&quot;Arial&quot;,&quot;sans-serif&quot;\">Mal schauen was passiert<o:p></o:p></span></p>\n" +
        "</div>\n" +
        "</body>\n" +
        "</html>\n";

    assertEquals("Before\nAfter", HTMLUtility.toPlainTextWithTable(in1));
    assertEquals("Before\nAfter", HTMLUtility.toPlainTextWithTable(in2));
    assertEquals("Before\nInBetween\nAfter", HTMLUtility.toPlainTextWithTable(in3));
    assertEquals("Before\nAfter", HTMLUtility.toPlainTextWithTable(in4));
    assertEquals("Before\n \nAfter", HTMLUtility.toPlainTextWithTable(in5));
    assertEquals("Before\nAfter", HTMLUtility.toPlainTextWithTable(in6));
    assertEquals("Sehr geehrte Damen und Herren\nKeine\n \nEine", HTMLUtility.toPlainTextWithTable(in7));
    assertEquals("Test\n \n\nVon: Bla, Bla Inc\nGesendet: Mittwoch, 14. Oktober 2015 14:01\nAn: Peter Muster, blub\nBetreff: Test Betreff\n\n \n-----Ursprüngliche Nachricht-----\nVon: peter.muster@bla.ch\nGesendet: 14.10.15 13:52\nAn: blablub < blablub.bla@hotmail.com >\nBetreff: Test E-Mail\n \nHallo\n\nDas ist ein Testmail\nKeine Leerzeile vor dieser Zeile\n\nVor dieser Zeile hats 1 Leerzeile\n\n \nDavor 2\n\n \n \nDavor 3\n\nMal schauen was passiert", cleanPlainText(HTMLUtility.toPlainTextWithTable(in8)));
  }

  public static String cleanPlainText(String text) {
    if (StringUtility.isNullOrEmpty(text)) {
      return null;
    }
    // trim body anyway; '\u00A0' = non-breaking space
    String s = text;
    // trim leading whitespace
    Matcher matcher = Pattern.compile("^[\\s\u00A0]*", Pattern.DOTALL).matcher(s);
    if (matcher.find()) {
      s = matcher.replaceAll("");
    }
    // trim trailing whitespace
    matcher = Pattern.compile("[\\s\u00A0]*$", Pattern.DOTALL).matcher(s);
    if (matcher.find()) {
      s = matcher.replaceAll("");
    }
    // ticket 158053: cleanup-operations are needed because of outlook-msg-line-endings
    matcher = Pattern.compile("\r\n\u00A0\r\n").matcher(s);
    if (matcher.find()) {
      // this is probably an outlook email - remove double-linebreaks
      s = s.replaceAll("\r\n\r\n", "\n");
      s = s.replaceAll("\n\u00A0\n", "\n\n");
    }
    return s;
  }

  /**
   * Test cases for {@link HTMLUtility#toPlainTextWithTable(String)}:
   * ensure that the line breaks in table cell will be ignored.
   *
   * @See Bug 415316
   */
  @Test
  public void testToPlainTextWithTableParagraphInTable() throws Exception {
    String input = "<table border=\"1\">\n" +
        "  <tr>\n" +
        "    <th class=\"xxx\">My <p>Header 1</th>\n" +
        "    <th>My <p>Header</p> 2</th >\n" +
        "  </tr>\n\r" +
        "  <tr>\n\r" +
        "    <td>row 1 p,<p> column</ P> 1</td>\n" +
        "    <td>\nrow 1,< P> column\n < p >2</td>\n" +
        "  </tr>\n" +
        "  <tr>\n" +
        "    <td >row td 2,<p/> column 1</td>\n" +
        "    <td valign=\"top\"><p >row 2, </p   >column 2</td>\n" +
        "  </tr>\n" +
        "  <tr>\n\r" +
        "    <td>row <P>3th,</p> column 1</td>\n" +
        "    <td>row 3th, <p>column </p> 2</td>\n" +
        "  </tr>\n" +
        "</table>";

    String expected = "My Header 1 | My Header 2 |\n" +
        "row 1 p, column 1 | row 1, column 2 |\n" +
        "row td 2, column 1 | row 2, column 2 |\n" +
        "row 3th, column 1 | row 3th, column 2 |";

    assertEquals(expected, HTMLUtility.toPlainTextWithTable(input));
  }

  /**
   * Test cases for {@link HTMLUtility#toPlainTextWithTable(String)}: test different lines breaks (p, br, table)
   *
   * @See Bug 415316
   */
  @Test
  public void testToPlainTextWithTableLineBreaks() throws Exception {
    String input;
    String expected;

    input = "Lorem<br>Ipsum<p>Dolor</p>Cosec<BR/><p>Adipi<br >Sagit\n</p>Sallu";
    expected = "Lorem\n" +
        "Ipsum\n" +
        "Dolor\n" +
        "Cosec\n\n" +
        "Adipi\n" +
        "Sagit\n" +
        "Sallu";
    assertEquals(expected, HTMLUtility.toPlainTextWithTable(input));

    input = "<p>Lorem<br>Ipsum\n</p>\n<TABLE class=\"atable\"><tr><td>Dolor</td><td>Cosec</td></tr></table><p>Adipi<BR>Sagit\n</p><table><tr><td>Sallu</td></tr></table>";
    expected = "Lorem\n" +
        "Ipsum\n" +
        "Dolor | " +
        "Cosec |" + "\n\n" +
        "Adipi\n" +
        "Sagit\n" +
        "Sallu |";
    assertEquals(expected, HTMLUtility.toPlainTextWithTable(input));
  }

  /**
   * Test cases for {@link HTMLUtility#toPlainTextWithTable(String)}: test null and empty values.
   */
  @Test
  public void testToPlainTextWithNullAndEmptyValues() throws Exception {
    String input;
    String expected;

    input = "";
    expected = "";
    assertEquals(expected, HTMLUtility.toPlainTextWithTable(input));

    input = null;
    expected = null;
    assertEquals(expected, HTMLUtility.toPlainTextWithTable(input));
  }
}
