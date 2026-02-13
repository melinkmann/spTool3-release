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

import core.SpTool3Main;
import javafx.application.Application;

/**
 * When compiling with jfx and jre to a single package, jfx is somehow not there during runtime. If
 * the main class extends Application  you get this error: Error: JavaFX runtime components are
 * missing, and are required to run this application
 * <p>
 * When executing this as main class and just calling {@link SpTool3Main#launch} it works.
 */
public class MainLauncher {

  public static final String VERSION = SpTool3Main.VERSION_ID;

  public static void main(String[] args) {
    Application.launch(SpTool3Main.class, args);
  }


}

