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

import java.util.function.Supplier;
import jdk.jfr.FlightRecorder;


class ClusterInfoMonitorImpl implements ClusterInfoMonitor {

  private final Runnable runnable;

  ClusterInfoMonitorImpl(Supplier<String> supplier) {
    this.runnable = ()-> {
      newEvent(supplier.get()).commit();
    };
  }
  
  private ClusterInfoEvent newEvent(String info) {
    return new ClusterInfoEvent(info);
  }
  
  @Override
  public void register() {
    FlightRecorder.addPeriodicEvent(ClusterInfoEvent.class, runnable);
  }

  @Override
  public void unregister() {
    FlightRecorder.removePeriodicEvent(runnable);
  }
}
