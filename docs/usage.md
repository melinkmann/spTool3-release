# Overview

## 1. Start spTool

Upon startup, you may start a lightweight version as a **data generator** only.
For the full version including analysis capabilities select **analyser**.
![img](images/startConfig.png){: width="150" style="display: block; margin: 0.5em auto 0.5em auto;"}

## 2. spTool main screen

![img](images/overview.png){: width="500" style="display: block; margin: 0.5em auto 0.5em auto;"}

### 2.1 Main menu bar (box 1)

#### 2.1.1 File menu:

- `Save project as`: Saves all sample sets with their samples, including imported raw data, processed particle data,
  quantification and method, to the drive.
    - Note: For ICP-TOFMS data, this does not save those isotopes that have not
      been loaded. If you move the raw data files, reprocessing of the data may cause issues (see
      `Check sample files`).
    - Note: The project does not save the current method in the method editor. Use the method editor to save and
      organise your method files. Alternatively, each sample contains a copy of the method that it has been
      processes with, and you can load the method from the sample after loading the project in the future with
      `Open project`. Learn how to organize your methods [here](methods.md).
- `Open project`: Load a project.
- `Save appearance`: Saves all settings affecting the user interface and plots. This includes:
    - All graph settings shown in `box 9`
    - Positions of dividers between user interface elements
    - Positions of popup views of the data view and legend panels (`box 8` and `box 9`)
- `Reset appearance`: Resets settings affecting the user interface and plots (see `Save appearance`) to their initial
  values.
- `Check sample files`: Starts a lookup whether the raw data files of the current samples can still be found on the
  computer. Else, it allows you to set a new drive location to search for the data and match them to the samples.
  Why are the raw data needed? For ICP-TOFMS data processing, some calculations require access to the raw data. This
  includes, e.g., the mass spectrum viewer and cluster analysis based on all m/z (isotopes).

- #### 2.1.2 Import menu:
    - `Select files`: Select files that are in the *default import path* which can be set in the configuration (`Edit` >
      `Configuration` > `Import path`). This opens the **'Select files' dialog**.
    - **'Select files' dialog**
      ![img](images/selectFiles.png){: width="400" style="display: block; margin: 0.5em auto 0.5em auto;"}
        - Select files **box A**: Enter a path (folder) where your data is located. Use copy/paste or manually enter a
          valid path. Alternatively use the **right-click menu** to open a file browser to set a directory.
        - Select files **box B**: Click this button to list the files in the directory.
        - Select files **box C**: Select this checkbox and specify a type (e.g., 'csv') if you only wish to be shown
          files of a certain type. Note: This checkbox also affects the drag/drop import of files into the main window
          of spTool.
        - Select files **box D**: Select this checkbox if you want to browse subdirectories, too. You can set the
          depth,
          i.e.,
          how many
          levels will be browsed in the configuration: `Edit` > `Configuration` > `Subdirectory import depth`. Note:
          This checkbox also affects the drag/drop import of files into the main window of spTool.
        - Select files **box E**: Clear the list of files below.
        - Select files **box F**: Enter keywords to filter the list of files shown below in **box G**.
        - Select files **box G**: This is the list of files. When you enter a keyword in the search field in **box F**,
          only a subset of the files is shown that matches the search. The file list has special features:

            - You can drag/drop files and folders here from the file browser on your system, e.g., Windows
              explorer. For drag/dropping directories, you can set the depth, i.e., how many levels will be browsed in
              the configuration: `Edit` > `Configuration` > `Subdirectory import depth`.
            - You can select files by clicking on them. Hold `Control` or `Shift` key to select individual files or a
              series of files, respectively. Double click selects all files.
            - **Special case:** If you want to use the search function and select multiple files for different keywords,
              you can use the **right click menu** in **box H**.

        - Select files **box H**: Selection right-click menu: You can mark files in the list as **selected** using this
          menu.  
          **Intended usage**: You browse a folder with multiple files but only want to import a subset. Enter a search
          keyword in **box F**, e.g., 'Au NP'. Now, only files that contain the keyword are shown. Right-click on
          the desired files in the list (**box G**) and click 'Select' in the popup menu that opens. Now you can change
          search key word and repeat the process.  
          *If you do not wish to use the select files right-click menu (box H), you can of course select files as you
          would in any other list.*
        - Finally, click the 'Continue' button or `Control` + `Enter` to continue.

    - ---
    - `Browse`: Opens a file browser popup to select a folder for import. Next, the **'Select files' dialog** is shown (
      see above `Select files`). Click the 'List files' button and use the options described above for the
      **'Select files' dialog**.
    - ---
    - `Recent folders`: This opens a popup list with locations (folders) that have been used for imports recently.
        - Click the **'Continue' button** or **double-click** on a folder to directly proceed to the
          **'List files' dialog**.
        - Right-click a directory to mark it as favorite or delete it.
        - Use the bin icon (top) to clear the list.
        - Use the save icon (top) to save changes (e.g., deletions or highlighting).
        - If a warning symbol (yellow triangle with exclamation mark) is shown in front of a folder, the directory does
          not exist anymore on your system.
    - ---
    - `Recent files`: This opens a popup list with sets of files that have been imported recently.
        - Click the 'Continue' button proceed to the **'List files' dialog**.
        - **Right-click** on an entry to mark it as a favorite or delete it.
        - Use the bin symbol at the top to clear the entire list.
        - Use the save symbol at the top to save changes (e.g., deletions or highlighting).
        - Use the **right-click** option **View content** to see which files the entry includes.
        - Select an entry and press `F2` or **left-click** on it again, or **double-click** on an entry.

- #### 2.1.3 Export menu:
  `Show dialog`: Opens the export dialog. **Note:** Most exports take into account your current selection of samples,
  isotopes and populations. In case you do not get the expected data export, double-check if your selection is correct.
  Here follows a brief overview of the most important export features:
    - **In the centre**, all export buttons that require further settings are shown together with these settings:
        - `Format`: Decide if data is copied to clipboard or written as a csv file. Note: If your export would be
          written as several csv files, you should not use the clipboard option.
        - `Export path`: If 'csv file' is selected, you can set the folder where files are written. Use the
          **right-click menu** to open a file browser to set a directory.
        - `Export raw data` button: Exports the raw data as time-resolved intensity data. You may add event markers (
          symbols that indicate the peak), population markers (symbols at the bottom that indicate which (in-silico)
          population an event belongs to) and tell spTool to only export the selected isotopes.
        - `Export custom event data`: Exports data for each event in a customisable fashion: This button also lets you
          decide if you also want to export the background (i.e., those data points that are not part of an event),
          e.g., to create a histogram of background and events to emphasise separation of particles from the background
          signal.
    - **On the right side**, there are buttons that do not need further settings:
        - `Export event data`: Exports data for each event including peak area. This export is comprehensive. Use
          the `Export custom event data` for a customisable and more lightweight export.
        - `Export results table`: Exports the table from the `TAB` pane with key results and settings.
        - `Export method as ...`: Exports the current method either as a human-readable '.csv' file or an '.spm' file
          that
          can be loaded into spTool later.
        - (... to be completed ...)

- #### 2.1.4 Edit menu:
    - `Submethod editor`: Opens the editor to create, modify and organise submethods.
    - `Configuration`: Opens the configuration editor.

- #### 2.1.5 Action menu:
    - `Increase dewll time`: Opens a submethod dialog to create a copy of each selected sample whose data is binned to
      the specified dwell time. This allows a rapid what-if exploration to see what the data would look like at longer
      dwell times.
    - `Cut time region`: Opens a submethod dialog to set limits on the time series, e.g., only use data from 20 s to 30
      s. You can also reset the sample to its original length with this action.
    - `Select event data range`: Opens a submethod dialog to set limits on the event data. This operation is the same as
      selecting a region-of-interest (ROI). This is a lightweight version of the ROI submethod.
    - `Compute isotope ratio`: Opens a submethod dialog to compute isotope ratios. For all selected samples, the ratio
      of the first two selected isotopes is computed. The division is in order, i.e., divide first selected isotope by
      second selected isotope. Use the 'Invert' checkbox in the dialog to invert the order, i.e., divide second by first
      selected isotope.

- #### 2.1.6 Tools menu:
    - `z/alpha converter`: Opens a popup to convert between the one-sided alpha error (Type I error probability) and the
      corresponding z-factor (as in z·σ) used to calculate a critical limit. Example: z = 1.645 --> α = 5%.
    - `Abundance calculator`: For the synthetic data generator. In the `Particle population parameters ` submethod, you
      have to enter the signal for each element (not per isotope). This calculator helps you determine which 'total
      signal' to specify in the submethod in order to achieve a certain isotope signal, assuming natural abundances. For
      example: In the field 'Isotope', type 13C. For 'Isotope signal', enter 100.
      The popup tells you that the 'Total signal' for C must be 9345.8 cts to obtain 100 cts for 13C.
    - `Isotope search`: A lightweight search window to find isotopes. Enter an element symbol (e.g., Gd) and get a list
      of all its isotopes. Enter a number (e.g., 156) and get a list of all elements with such isotope. You may also
      enter an 'interfering mass shift' (such as 16 for oxides) and include 'Doubly charged' ions as well as 'Dimers'.
      This tool helps to plan and inspire in-silico experiments with interferences, and can also help to better
      understand interferences in experimental data. The report looks like this:
      ![img](images/isotopeSearch.png){: width="500" style="display: block; margin: 0.5em auto 0.5em auto;"}
    - `Interference library`: Search the interference database by Madeleine C. Lomax-Vogt, Fang Liu, John W.
      Olesik at [https://doi.org/10.1016/j.sab.2021.106098](https://doi.org/10.1016/j.sab.2021.106098)
    - `List isobars`: Lists isobaric isotopes to plan in-silico experiments or interpret experimental data.
    - `Significance test`: Quickly test results for statistical significance. Note: This feature is still under
      validation. *It is recommended to verify results against established statistical libraries, such as those provided
      in the R environment.*
    - `Estimate a- and b-error`: Compare in-silico data with data processing results to benchmark the data processing
      algorithm and settings.

- #### 2.1.7 Help menu:
    - `About`

### 2.2 Start/stop section (box 2)

- `Create`: If your [method](methods.md) contains in-silico data generator submethods to create synthetic data, this
  button will 1) execute the data generation and 2) run data analysis submethods (baseline, event search, ...) if
  available.
- Use the n = ... field to create replicates by executing the same method multiple times.
- `Process`: This button skips all data generator [submethods](methods.md) and only executes the analysis submethods (
  baseline, event search, ...). Use this button to reprocess synthetic data or to analyse experimental data.
- `Stop`: Tries to stop the current operation if possible.

### 2.3 Graph selection (box 3)

Decide which graph is shown by selecting the respective button.
**Use right-click to open multiple graphs as popup windows.**
You can decide which buttons are available via the configuration: `Edit` > `Configuration` > `Show ...`.

### 2.4 Sample set list (box 4)

Organise samples in sets. Use the **right-click menu** to `Create` or `Delete` sets. Populate sets by dragging/dropping
selected samples from `box 5` into sample sets. Hold control key to copy and shift key to cut. Use the text field at
the top to filter the sets that are shown for a keyword.

### 2.5 Sample table (box 5)

This table shows all samples within the currently selected set. Double click selects all samples. You can edit the
nickname with the `F2` key or by left-clicking on the nickname cell of a selected sample. You can also edit the comment
cell in the same way. `# NMP` gives the average number of nano/microparticles of the sample for the currently selected
channels (`box 7`) and population (`box 6`). The sample table has an extensive **right-click menu**:

![img](images/sampleTableContextMenu.png){: width="150" style="display: block; margin: 0 auto;"}

- `Favorite`/`Undo`: Highlight selected samples as a favorite. The sample table `box 5` can be sorted by the highlight
  property which is shown via a star symbol in the first column.
- `Clone`: create a copy of the sample, e.g., to compare different processing settings.
- `Comment`: Edit the comment of a sample in a convenient popup text editor. You can also edit the comment in the table
  column itself. If multiple samples are selected this will only affect the first of them.
- `Color`: Select the color of the selected sample. If multiple samples are selected this will only affect the first of
  them.
- `Load method`: Overwrites the current [method](methods.md) which is shown in the `MET` tab in the selection area
  `box 3` by loading the method that has been used to process the currently selected sample. If multiple samples are
  selected the first will be used.
- `View method`: Opens a popup window to view the [method](methods.md) that was used to process the currently selected
  sample in a read-only mode. If multiple samples are selected the first will be used.
- `Adjust`: Opens a popup window to edit the [method](methods.md) of the (first) currently selected sample and to
  reprocess it. You can also use this to change the isotopes that have been loaded from raw data during import by
  changing the selected isotopes and clicking `Update m/z`; this will keep the existing sample including quantification,
  nickname, comments, ... and only update the set of imported isotopes. Alternatively, you can import the raw data again
  as a new sample using `Import copy` or replace the sample entirely by re-loading the data using `Replace`.

  ![img](images/reprocessPopup.png){: width="550" style="display: block; margin: 0.5em auto 0.5em auto;"}

- `Reprocess`: Reprocess the selected samples. When you process a sample, a copy of the [method](methods.md) that was
  used for processing is stored in the sample. This button triggers reprocessing using the method that was previously
  stored with the sample. This option can help to, reprocess a sample after manually removing isotopes or to quickly
  update samples when the algorithms have received updates without changing the submethod parameters.
- `Delete Samples Globally`: Deletes the selected samples from all sets. This is the 'delete' behaviour that will
  permanently remove a sample from the project.
- `Remove Samples from Set`: Removes the selected samples from the currently selected set in `box 4`. If a sample
  exists in other sets, it will stay there.

**Further features of the sample table**:

- You can drag/drop selected samples from `box 5` into sample sets. Hold control key to copy and shift key to cut.
- `Group`/`Undo grouping`: Merge multiple sample files into a single combined sample. This feature serves to merge
  individual sample files into one combined sample.

### 2.6 Population list (box 6)

spTool structures events into populations. Typical populations are:

- 'Synthetic population': These are all events that have been generated by the in-silico data generator.
- 'Mass: Peak search': These are all events that have been found by a 'Peak search'. 'Mass' is a tag that can be set in
  the search [submethod](methods.md).

The **right-click menu** has additional options:

- `Lock`: If a population is locked, it will always remain selected. If a sample does not have a specific population, a
  warning symbol will be shown to indicate that this population does not exist, and it will not be shown in any graph or
  table.
- `Unlock`: Unlocks a population.
- `Delete`: Deletes a population from all selected samples.
- `View mz`: For aligned populations, this button opens a list with all contributing isotopes.

### 2.7 Channel (Isotope) table  (box 7)

Select channels (i.e., m/z or isotopes). At the top, the dropdown menu decides if histograms and other graphs show the
raw intensity in counts (cts) or size and mass units if quantification is provided.

The **right-click menu** has additional options:

- `Default`: Selects the default isotopes for each element. The 'default' usually is the most abundant isotope but this
  can be set in the configuration:  `Edit` > `Configuration` > `Default isotope`. Double-clicking on the table has the
  same effect.
- `Select all`: Does what it says.
- `Fix isotopes`: Opens a periodic table popup view. For the selected sample, you can specify a 'fixed' set of isotopes
  that are different from the `Default` option. Use `Control` key and a double click on the table to select the 'fixed'
  isotopes.
- `Color`: Assign a custom color to a channel. This can also be edited at `Edit` > `Configuration` > `Isotope color`.
  Note: In order to save the changes that you make via the channel table in `box 7`, you must open the configuration
  editor `Edit` > `Configuration` and save it (either via the symbol at the top left or the 'Save & close' button).
- `Delete isotopes`: Permanently removes the selected isotopes from the selected samples. Note: Results from processing,
  e.g., combined significance will not be deleted retrospectively. It is recommended to reprocess the samples, either
  using the right click `Reprocess` right click menu button from `box 5` (use this if each sample has its own method
  parameters) or the main `Process` button in `box 2` (use this if all samples share the same method parameters).

![img](images/isotopeContextMenu.png){: width="125" style="display: block; margin: 0 auto;"}

### 2.8 Data view and legend panel (box 8)

Shows the `Start` page, the `MET` [method](methods.md) editor and **all graphs**. Use `box 3` to decide which type of
graph is shown. Use the right click menu of the graph area to export the data as csv, copy data to clipboard, save the
graphic as a file or copy the graph to clipboard as an image. Legend and graph are rendered in different ways which is
why they are written into 2 distinct files.

- **Plot Navigation** (see `Start` panel)
    - **Go back to previous view**: `Control` + `Z`
    - **Reset graph entirely**: Click left mouse and drag up
- **Move the plot**
    - Option A: `Alt` + 1× left mouse click, then move the mouse
    - Option B: 2× left mouse click, then drag the mouse
- **Zoom**
    - Option A: Scroll wheel → zoom both x and y axes
    - Option B: Scroll wheel + `Control` → zoom x-axis
    - Option C: Scroll wheel + `Alt` → zoom y-axis
- **Arrow keys** (click on the plot to activate arrow navigation)
    - Arrow keys (Left/Right/Up/Down): Move the plot
    - `Control` + `Left/Right`: Zoom in/out for x axis
    - `Control` + `Up/Down`: Zoom in/out for y axis
- **Dragging zoom**
    - `Control` + `Right mouse`: Zoom x axis by dragging
    - `Shift` + `Left mouse`: Zoom y axis by dragging
- **Transfer values from plots**
    - Double left-click on a graph to copy current mouse position
    - Certain text fields in the user interface accept values when double clicking on the text field while pressing the
      `Control` key

### 2.9 Graph settings (box 9)

Each graph has its own set of settings. **Most importantly**, histograms, box plots, scatter plots, ... allow you to
select which **event parameter** you would like to visualise. Take a look at this example from the histogram settings:

![img](images/eventProperty.png){: width="225" style="display: block; margin: 0 auto;"}

- `Area`: Shows the peak 'area' (actually the sum of all data points that form the event). No background subtraction is
  applied.
- `Net area`: Shows the net peak 'area', i.e., remaining peak area after background subtraction. This is the event
  parameter that you would usually want to use for quantification.
  Take a look at the 'Search' [submethod](methods.md)'s options: Here you can decide how background is computed.
- `Height`: Shows the intensity value of the highest data point of the peak.
- `Net height`: Shows the intensity value of the highest data point of the peak minus the background signal. For
  example, if the background is estimated via the mean of the baseline (see 'Search' [submethod](methods.md)),
  then the net is `height - mean baseline signal`.
- `Duration`: Duration of the peak (from first to last data point, i.e., peak width at base) in µs.
- `Background per event`: In the 'Search' [submethod](methods.md) you can decide how background is computed. This option
  allows you to plot the background signal that was determined for each event. The background per event specifies the
  estimated summed background signal for the whole peak (not per data point)! Thus, for a constant baseline, wider peaks
  will have more 'background per event' than thin peaks.
- `Number of data points`: Width of the peak (from first to last data point, i.e., peak width at base) in number of data
  points.
- `Asymmetry`: Event peak asymmetry is computed as `2b / (a+b)`, where `a` is the distance from start to apex and `b` is
  the
  distance from apex to end. It is equal to `1` when `a = b`, and increases as `b` becomes more pronounced relative to
  `a`.

### 2.10 Status panel (box 10)

This panel shows the name of the current [method](methods.md) and important system performance parameters:

- `Temp`: spTool uses temporary files to reduce memory (RAM) usage. These files are stored on your machine, typically in
  temporary directory on the main drive of the operating system. The text below shows how many GB of temporary files
  spTool has created so far.
  **Note**: To delete these temporary files, close and then restart spTool once!** Otherwise, the files will
  likely remain in the temporary directory and continue to occupy disk space until the next time spTool is started.
- `Memory`: Shows an estimate of the current RAM usage of the session. spTool automatically decides when to release
  memory, so high RAM usage is not necessarily a cause for concern.
- `Progress`: Estimates progress of the current operation. **Note: Saving and loading projects do not update the
  progress bar. Although it may appear to be stuck, the operation is still running**.

---



****
***Find the latest release on [github](https://github.com/melinkmann/spTool3-release/releases/latest)!*** 