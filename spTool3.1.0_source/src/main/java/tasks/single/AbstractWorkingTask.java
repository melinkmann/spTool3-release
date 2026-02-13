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

package tasks.single;

import com.google.common.util.concurrent.AtomicDouble;
import java.util.concurrent.atomic.AtomicBoolean;
import tasks.WorkingTask;

public abstract class AbstractWorkingTask implements WorkingTask {

  private final String taskName;
  private final AtomicDouble progress;
  private final AtomicBoolean isStopped;


  public AbstractWorkingTask(String taskName) {
    this.taskName = taskName;
    this.progress = new AtomicDouble(0);
    this.isStopped = new AtomicBoolean(false);
  }

  public String getTaskName() {
    return taskName;
  }

  // Progress notifier
  @Override
  public AtomicDouble getProgress() {
    return progress;
  }

  protected void setProgress(double progress) {
    this.progress.set(progress);
  }

  // Stop, i.e. interrupt.
  @Override
  public void stop() {
    isStopped.set(true);
  }

  public AtomicBoolean getIsStopped() {
    return isStopped;
  }

}
