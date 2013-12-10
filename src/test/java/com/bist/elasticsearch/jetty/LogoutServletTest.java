/*
 * Copyright 2013 Borsa Istanbul A.S.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bist.elasticsearch.jetty;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpSession;

import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: zeldal.ozdemir
 * Date: 20.11.2013
 * Time: 09:45
 * To change this template use File | Settings | File Templates.
 */
public class LogoutServletTest {
    @Test
    public void testDoGet() throws Exception {
        Request request = mock(Request.class);
        Response response = mock(Response.class);
        HttpSession session = Mockito.mock(HttpSession.class);

        when(request.getSession(false)).thenReturn(session) ;

        LogoutServlet logoutServlet = new LogoutServlet();
        logoutServlet.doGet(request, response);


        verify(request).getSession(false);
        verify(session).invalidate();
        verify(response).sendRedirect("/");
    }

    @Test
    public void testDoGetWithNoSession() throws Exception {
        Request request = mock(Request.class);
        Response response = mock(Response.class);

        when(request.getSession(false)).thenReturn(null) ;

        LogoutServlet logoutServlet = new LogoutServlet();
        logoutServlet.doGet(request, response);

        verify(request).getSession(false);
        verify(response).sendRedirect("/");
    }
}
