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

package tasks.batch;

import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tasks.Task;
import tasks.TaskResult;

public class SimpleLinearBatch<T> extends AbstractLinearQueue {

  private static final Logger LOGGER = LogManager.getLogger(SimpleParallelBatch.class);

  private final TaskResult onQueueFinished;

  public SimpleLinearBatch(String taskName,
      List<Task> tasks,
      boolean isRestricted,
      TaskResult onQueueFinished) {
    super(taskName, isRestricted, tasks);
    this.onQueueFinished = onQueueFinished;
  }

  public SimpleLinearBatch(String taskName,
      Task task,
      boolean isRestricted,
      TaskResult onQueueFinished) {
    super(taskName, isRestricted, Collections.singletonList(task));
    this.onQueueFinished = onQueueFinished;
  }


  @Override
  public TaskResult call() {
    // Do the work
    super.callWithoutResult();
    // Note: Here, we could as well implement a version that does something with more specific TaskResults.

    // Then return the specified result which will be called by the TaskManager.
    // Note that this will be called by the TaskManager thread and it should be computationally cheap.
    return onQueueFinished;
  }
}


