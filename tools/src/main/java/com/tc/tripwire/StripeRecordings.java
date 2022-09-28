/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.tripwire;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

/**
 *
 */
public class StripeRecordings {
  private final List<Path> sources;
  private Path active;
  private List<Path> passives;

  public StripeRecordings(List<Path> files) throws IOException {
    sources = files;
  }

  private final void mapFiles(List<Path> files) throws IOException {
    for (Path f : files) {
      String state = findFirstState(new RecordingFile(f));
      switch (state) {
        case "ACTIVE-COORDINATOR":
          active = f;
        default:
          passives.add(f);
      }
    }
  }

  private String findFirstState(RecordingFile file) throws IOException {
    while (file.hasMoreEvents()) {
      RecordedEvent event = file.readEvent();
      if (event.getEventType().getLabel().equals("ServerState")) {
        String state = event.getString("state");
        if (state.startsWith("ACTIVE")) {
          return state;
        } else if (state.startsWith("PASSIVE")) {
          return state;
        }
      }
    }
    return "UNKNOWN";
  }


  private boolean isLoggable(RecordedEvent event) {
    String type = event.getEventType().getName();
    return (type.equals("org.terracotta.tripwire.LogEvent") || type.equals("com.tc.objectserver.entity.ReplicationEvent"));
  }

  public void printStripeLogs() throws IOException {
    if (sources.isEmpty()) {
      return;
    }
    List<ServerRecording> servers = sources.stream().map(ServerRecording::new).collect(Collectors.toList());
    Iterator<ServerRecording> it = servers.iterator();
    Stream<LoggableRecord> base = it.next().sortedLogs();
    while (it.hasNext()) {
      base = Stream.concat(base, it.next().sortedLogs());
    }
    base.sorted().filter(LoggableRecord::isLog).forEach(System.out::println);
  }
}
