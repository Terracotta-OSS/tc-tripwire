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
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

/**
 *
 */
public class ServerRecording {
  private final Path file;
//  private static final Logger LOGGER = LoggerFactory.getLogger(ServerRecording.class);
  public static final DateTimeFormatter FORMAT = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.LONG);
  private boolean active;
  private String level = "  ";

  public ServerRecording(Path file) {
    this.file = file;
  }

   Stream<RecordedEvent> streamify() throws IOException {
    RecordingFile rf = new RecordingFile(this.file);
    return StreamSupport.stream(new Spliterator<RecordedEvent>() {
      @Override
      public boolean tryAdvance(Consumer<? super RecordedEvent> action) {
        if (rf.hasMoreEvents()) {
          try {
            action.accept(rf.readEvent());
          } catch (IOException exp) {
            throw new UncheckedIOException(exp);
          }
          return true;
        } else {
          return false;
        }
      }

      @Override
      public Spliterator<RecordedEvent> trySplit() {
        return null;
      }

      @Override
      public long estimateSize() {
        return Long.MAX_VALUE;
      }

      @Override
      public int characteristics() {
        return 0;
      }
    }, false);
  }

  public Stream<LoggableRecord> sortedLogs() throws IOException {
    AtomicLong session = new AtomicLong(-1L);
    AtomicLong seq = new AtomicLong(-Math.abs(file.getFileName().toString().hashCode()));
    return streamify().filter(this::isLoggable).sorted((a,b)->a.getStartTime().compareTo(b.getStartTime())).map(r -> {
      if (isReplication(r)) {
        session.set(r.getLong("session"));
        seq.set(r.getLong("sequence"));
        setActive(r);
      }
      return new LoggableRecord(session.get(), seq.get(), r);
    });
  }

  public void printAllLogs() throws IOException {
    streamify().filter(ServerRecording::isLog).sorted((a, b)->a.getStartTime().compareTo(b.getStartTime())).forEachOrdered(this::printLog);
  }

  private void printLog(RecordedEvent event) {
    System.out.printf("%s%s - %s:%s -- %s\n", level, FORMAT.format(event.getStartTime().atZone(ZoneId.systemDefault())), event.getString("level"), event.getString("name"), event.getString("statement"));
  }

  public void setLevel(int lev) {
    char[] spaces = new char[lev*2];
    Arrays.fill(spaces, ' ');
    level = new String(spaces);
  }

  public boolean isActive() {
    return active;
  }

  private void setActive(RecordedEvent event) {
    boolean setting = (event.getLong("session") >= 0);
    if (setting != active) {
      setLevel(setting ? 0 : 1);
      active = setting;
    }
  }

  private boolean isLoggable(RecordedEvent event) {
    String type = event.getEventType().getName();
    return  type.equals("org.terracotta.tripwire.LogEvent") || type.equals("org.terracotta.tripwire.ReplicationEvent");
  }

  static boolean isLog(RecordedEvent event) {
    return (event.getEventType().getName().equals("org.terracotta.tripwire.LogEvent"));
  }

  static boolean isReplication(RecordedEvent event) {
    return (event.getEventType().getName().equals("org.terracotta.tripwire.ReplicationEvent"));
  }
}
