/*
 *  Copyright 2026 Matthias Elinkmann, spTool3
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package processing.options;

import org.apache.logging.log4j.Level;

public enum LogLevel {

  /**
   * A fatal event that will prevent the application from continuing.
   */
  FATAL {
    @Override
    public String toString() {
      return "Fatal";
    }

    @Override
    public Level getLevel() {
      return Level.FATAL;
    }
  },

  /**
   * An error in the application, possibly recoverable.
   */
  ERROR {
    @Override
    public String toString() {
      return "Error";
    }

    @Override
    public Level getLevel() {
      return Level.ERROR;
    }
  },
  /**
   * An event that might possible lead to an error.
   */
  WARN {
    @Override
    public String toString() {
      return "Warning";
    }

    @Override
    public Level getLevel() {
      return Level.WARN;
    }
  },
  /**
   * An event for informational purposes.
   */
  INFO {
    @Override
    public String toString() {
      return "Info";
    }

    @Override
    public Level getLevel() {
      return Level.INFO;
    }
  },
  /**
   * A general debugging event.
   */
  DEBUG {
    @Override
    public String toString() {
      return "Debug";
    }

    @Override
    public Level getLevel() {
      return Level.DEBUG;
    }
  },
  /**
   * A fine-grained debug message, typically capturing the flow through the application.
   */
  TRACE {
    @Override
    public String toString() {
      return "Trace";
    }

    @Override
    public Level getLevel() {
      return Level.TRACE;
    }
  };

  public abstract Level getLevel();


}
