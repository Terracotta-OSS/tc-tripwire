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
package org.terracotta.tripwire;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

public class EventAppender extends AppenderBase<ILoggingEvent> {
  
  private final static boolean ENABLED;
  
  static {
    boolean check = false;
    try {
      Class.forName("jdk.jfr.Event");
      check = true && !Boolean.getBoolean("tripwire.logging.disable");
    } catch (ClassNotFoundException cnf) {
    }
    ENABLED = check;
  }
  
  public EventAppender() {
  }

  @Override
  protected void append(ILoggingEvent e) {
    if (ENABLED) {
      new LogEvent(e.getLoggerName(), e.getLevel().toString(), e.getFormattedMessage()).commit();
    }
  }
  
  public static boolean isEnabled() {
    return ENABLED;
  }
}
