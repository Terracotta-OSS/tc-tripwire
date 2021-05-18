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

import static com.tc.tripwire.ServerRecording.FORMAT;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import jdk.jfr.EventType;
import jdk.jfr.consumer.RecordedEvent;

/**
 *
 */
public class LoggableRecord implements Comparable {
  private final long session;
  private final long sequence;
  private final RecordedEvent event;

  public LoggableRecord(long session, long sequence, RecordedEvent event) {
    this.session = session;
    this.sequence = sequence;
    this.event = event;
  }

  @Override
  public int compareTo(Object o) {
    if (o instanceof LoggableRecord) {
      LoggableRecord other = (LoggableRecord)o;
      long diff = session - other.session;
      if (diff == 0) {
        long seq = sequence - other.sequence;
        if (seq == 0) {
          return this.event.getStartTime().compareTo(other.event.getStartTime());
        } else {
          return seq < 0 ? -1 : 1;
        }
      } else {
        return diff < 0 ? -1 : 1;
      }
    }
    return 0;
  }

  public boolean isActive() {
    return session >= 0;
  }

  public long getSession() {
    return session;
  }

  public long getSequence() {
    return sequence;
  }

  public RecordedEvent getEvent() {
    return event;
  }

  public EventType getEventType() {
    return event.getEventType();
  }

  public Instant getStartTime() {
    return event.getStartTime();
  }

  public Instant getEndTime() {
    return event.getEndTime();
  }

  public Duration getDuration() {
    return event.getDuration();
  }

  @Override
  public String toString() {
    StringBuilder text = new StringBuilder();
    if (!isActive()) {
      text.append("   ");
    }
    text.append(FORMAT.format(event.getStartTime().atZone(ZoneId.systemDefault())));
    text.append(" - ");
    text.append(event.getString("level"));
    text.append(":");
    text.append(event.getString("name"));
    text.append("  --  ");
    text.append(event.getString("statement"));
    return text.toString();
  }
}
