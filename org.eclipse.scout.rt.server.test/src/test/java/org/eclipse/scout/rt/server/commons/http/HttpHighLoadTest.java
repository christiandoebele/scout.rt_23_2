/*******************************************************************************
 * Copyright (c) 2018 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.server.commons.http;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.scout.rt.platform.job.Jobs;
import org.eclipse.scout.rt.platform.util.IOUtility;
import org.eclipse.scout.rt.platform.util.ObjectUtility;
import org.eclipse.scout.rt.server.commons.servlet.AbstractHttpServlet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;

/**
 * Test how the Google HTTP Client API together with the Apache HTTP Client handle a high frequency of requests.
 */
public class HttpHighLoadTest {
  private TestingHttpClient m_client;
  private TestingHttpServer m_server;

  @Before
  public void before() {
    m_client = new TestingHttpClient();
    m_server = new TestingHttpServer(TestingHttpPorts.PORT_33004, "/", getClass().getResource("/webapps/" + getClass().getSimpleName()));
    m_server.start();
  }

  @After
  public void after() {
    m_client.stop();
    m_server.stop();
  }

  @Test
  public void testPost() throws IOException {
    HttpRequestFactory reqFactory = m_client.getHttpRequestFactory();
    int n = 10000;
    AtomicInteger successCount = new AtomicInteger();
    for (int i = 0; i < n; i++) {
      String arg = "" + i;
      byte[] reqBytes = arg.getBytes();
      Jobs.schedule(() -> {
        HttpRequest req = reqFactory.buildPostRequest(new GenericUrl(m_server.getContextUrl() + "hf"), new HttpContent() {
          @Override
          public void writeTo(OutputStream out) throws IOException {
            out.write(reqBytes);
          }

          @Override
          public boolean retrySupported() {
            return false;
          }

          @Override
          public String getType() {
            return "text/plain;charset=UTF-8";
          }

          @Override
          public long getLength() throws IOException {
            return reqBytes.length;
          }
        });
        HttpResponse resp = req.execute();
        String respText;
        try (InputStream in = resp.getContent()) {
          int len = ObjectUtility.nvl(resp.getHeaders().getContentLength(), -1L).intValue();
          byte[] respBytes = IOUtility.readBytes(in, len);
          respText = new String(respBytes, StandardCharsets.UTF_8).trim();
        }
        assertEquals(respText, "Post " + arg);
        successCount.incrementAndGet();
      }, Jobs.newInput().withExecutionHint("testPost"));
    }
    Jobs.getJobManager().awaitFinished(f -> f.containsExecutionHint("testPost"), 10, TimeUnit.MINUTES);
    assertEquals(n, successCount.get());
  }

  /**
   * http://172.0.0.1:33xyz/hf
   */
  public static class Servlet extends AbstractHttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      String arg = req.getParameter("arg");

      resp.setContentType("text/plain;charset=UTF-8");
      resp.getOutputStream().println("Get " + arg);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      assertEquals("text/plain;charset=UTF-8", req.getContentType());
      assertEquals("UTF-8", req.getCharacterEncoding());
      String arg = IOUtility.readString(req.getInputStream(), req.getCharacterEncoding(), req.getContentLength());

      resp.setContentType("text/plain;charset=UTF-8");
      resp.getOutputStream().println("Post " + arg);
    }
  }
}
