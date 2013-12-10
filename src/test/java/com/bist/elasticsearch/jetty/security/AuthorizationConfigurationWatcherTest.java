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

package com.bist.elasticsearch.jetty.security;

import org.elasticsearch.env.Environment;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.mockito.verification.ConstructorArgumentsVerification;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.net.URL;

import static org.powermock.api.mockito.PowerMockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: zeldal.ozdemir
 * Date: 20.11.2013
 * Time: 13:55
 * To change this template use File | Settings | File Templates.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(AuthorizationConfigurationWatcher.class)
public class AuthorizationConfigurationWatcherTest {

    @Test
    public void testStartWatching() throws Exception {
        Runnable runnable = mock(Runnable.class);

        AuthorizationConfigurationWatcher configurationWatcher = PowerMockito.spy(new AuthorizationConfigurationWatcher());
//        AuthorizationConfigurationWatcher spy = PowerMockito.spy(configurationWatcher);

        doReturn(runnable).when(configurationWatcher, "getRunnableForWatcher");

        Environment environment = mock(Environment.class);
        AuthorizationConfigurationLoader configurationLoader = mock(AuthorizationConfigurationLoader.class);
        QueryConstraints queryConstraints = mock(QueryConstraints.class);
        URL url = PowerMockito.mock(URL.class);
        Thread configThread = mock(Thread.class);


        configurationWatcher.setEnvironment(environment);
        String configFilePath = "ConfigFilePath";
        configurationWatcher.setAuthorizationMappingFile(configFilePath);
        whenNew(AuthorizationConfigurationLoader.class).withNoArguments().thenReturn(configurationLoader);

        when(environment.resolveConfig(configFilePath)).thenReturn(url);
        when(configurationLoader.load(url)).thenReturn(queryConstraints);


        whenNew(Thread.class).withArguments(runnable).thenReturn(configThread);


        configurationWatcher.startWatching();

        Mockito.verify(configThread).start();

        assert configThread == Whitebox.getInternalState(configurationWatcher,"configThread");


    }


    @Test
    public void testStartWatching_WithAlreadyWatching() throws Exception {

        AuthorizationConfigurationWatcher configurationWatcher = new AuthorizationConfigurationWatcher();
        Thread configThread = mock(Thread.class);
        Whitebox.setInternalState(configurationWatcher,"configThread", configThread);

        when(configThread.isAlive()).thenReturn(true);

        configurationWatcher.startWatching();

//        Mockito.verifyNoMoreInteractions(configThread,configurationWatcher);

    }

    @Test
    public void testStop() throws Exception {
        AuthorizationConfigurationWatcher configurationWatcher = new AuthorizationConfigurationWatcher();
        Thread configThread = mock(Thread.class);
        Whitebox.setInternalState(configurationWatcher,"configThread", configThread);

        when(configThread.isAlive()).thenReturn(true);
        configurationWatcher.stop(); // nothing has happened

        Mockito.verify(configThread).interrupt();
    }

    @Test
    public void testStopIfItsnotStarted() throws Exception {
        AuthorizationConfigurationWatcher configurationWatcher = new AuthorizationConfigurationWatcher();
        configurationWatcher.stop(); // nothing has happened
    }
}
