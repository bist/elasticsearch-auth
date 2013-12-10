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

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.elasticsearch.env.Environment;

import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: zeldal.ozdemir
 * Date: 15.11.2013
 * Time: 11:55
 * To change this template use File | Settings | File Templates.
 */
public class AuthorizationConfigurationWatcher {
    private static final Logger LOG = Log.getLogger(AuthorizationConfigurationWatcher.class);

    private String authorizationMappingFile;
    private Environment environment;
    private Thread configThread;
    private URL url;
    private AuthorizationConfigurationLoader authorizationConfigurationLoader;
    private QueryConstraints queryConstraints;

    public void startWatching(){
        if(configThread != null && configThread.isAlive())
            return;

        authorizationConfigurationLoader = new AuthorizationConfigurationLoader();

        url = environment.resolveConfig(authorizationMappingFile);

        queryConstraints = authorizationConfigurationLoader.load(url);

        configThread = new Thread(getRunnableForWatcher());
        configThread.start();
    }

    private Runnable getRunnableForWatcher() {
        return new Runnable() {
            @Override
            public void run() {
                startWatchingInternal();
            }
        };
    }

    public void stop(){
        if(configThread != null && configThread.isAlive() && !configThread.isInterrupted())
            configThread.interrupt();

    }

    private void startWatchingInternal() {
        try {
            Path authorizationMappingFilePath = Paths.get(url.toURI());

            WatchService watchService = getWatchService(authorizationMappingFilePath);

            for (; ; ) {
                checkChange(authorizationMappingFilePath, watchService);
                if(Thread.currentThread().isInterrupted())
                    break;
            }
        } catch (Exception e) {
            LOG.warn("Error while Watching File" + e.getMessage(), e);
        }
    }

    private WatchService getWatchService(Path authorizationMappingFilePath) throws IOException {
        Path parentPath = authorizationMappingFilePath.getParent();
        WatchService watchService = FileSystems.getDefault().newWatchService();
        parentPath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
        return watchService;
    }

    private void checkChange(Path authorizationMappingFilePath, WatchService watchService) throws InterruptedException {
        WatchKey watchKey = watchService.take();
        List<WatchEvent<?>> events = watchKey.pollEvents();
        if (events != null)  {
            for (WatchEvent<?> event : events) {
                if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                    WatchEvent<Path> modifyEvent = (WatchEvent<Path>) event;
                    Path resolved = authorizationMappingFilePath.getParent().resolve(modifyEvent.context());
                    if (resolved.equals(authorizationMappingFilePath)) {
                        LOG.warn("Loading Updated AuthorizationMappingFile:" + url);
                        queryConstraints = authorizationConfigurationLoader.load(url);
                    }
                }
            }
            watchKey.reset();
        }
    }


    public void setAuthorizationMappingFile(String authorizationMappingFile) {
        this.authorizationMappingFile = authorizationMappingFile;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public QueryConstraints getQueryConstraints() {
        return queryConstraints;
    }
}
