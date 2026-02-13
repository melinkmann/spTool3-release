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

package visualizer;

import gui.util.UiUtil;
import java.net.URL;
import javafx.concurrent.Worker;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

// StackPane --> Resizing
public class Browser extends StackPane {

  final WebView browser = new WebView();
  final WebEngine webEngine = browser.getEngine();

  final HBox btnBox;
  final Button zoomInButton = UiUtil.getToolbarBtn("/img/zoomIn.png", "Zoom in");
  final Button zoomOutButton = UiUtil.getToolbarBtn("/img/zoomOut.png", "Zoom out");
  final Button resetZoomButton = UiUtil.getToolbarBtn("/img/resetZoom.png", "Reset zoom");

  public Browser() {
    //apply the styles
    getStyleClass().add("browser");
    // load the web page
    String pdfFilePath = "/doc/readme.html";
    URL resource = UiUtil.class.getResource(pdfFilePath);
    webEngine.load(resource.toExternalForm());
    //add the web view to the scene
    getChildren().add(browser);
    disableInternalWebViewDnD(browser);

    String modernLightCss = """
        /* Typography & rhythm */
        :root{
          --content-max: 1540px;
          --line-height: 1.6;
          --font-size: 17px;
          --fg: #111;
          --muted: #6b7280;
          --bg: #fff;
          --code-bg: #f6f8fa;
          --border: #e5e7eb;
          --link: #0b5fff;
          --link-visited: #6b4eff;
          --accent: #0b5fff;
        }
                        
        html { scroll-behavior: smooth; }
        * { box-sizing: border-box; }

        body{
          margin: 2.2rem auto;
          padding: 0 1rem;
          max-width: var(--content-max);
          color: var(--fg);
          background: var(--bg);
          font-size: var(--font-size);
          line-height: var(--line-height);
          /* Linux-first font stacks */
          font-family:
            "DejaVu Sans", "Noto Sans", "Liberation Sans",
            system-ui, -apple-system, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
          -webkit-font-smoothing: antialiased;
          -moz-osx-font-smoothing: grayscale;
        }

        /* Headings */
        h1, h2, h3, h4{
          line-height: 1.25;
          margin: 2rem 0 0.75rem;
          font-weight: 700;
          letter-spacing: .2px;
        }
        h1{ font-size: 2rem; margin-top: 0; }
        h2{ font-size: 1.6rem; border-bottom: 1px solid var(--border); padding-bottom: .3rem; }
        h3{ font-size: 1.25rem; }
        h4{ font-size: 1.05rem; color: var(--muted); }

        /* Paragraphs & lists */
        p{ margin: 0 0 1rem; }
        ul, ol{ margin: .5rem 0 1rem 1.4rem; }
        li{ margin: .25rem 0; }

        /* Blockquotes */
        blockquote{
          margin: 1rem 0;
          padding: .8rem 1rem;
          border-left: 4px solid var(--accent);
          background: #f9fafb;
        }
        blockquote p{ margin: .3rem 0; }

        /* Links */
        a{ color: var(--link); text-decoration: none; }
        a:hover{ text-decoration: underline; }
        a:visited{ color: var(--link-visited); }

        /* Images */
        img{
          display: block;
          margin: 1rem auto;
          max-width: 100%;
          height: auto;
          border-radius: 4px;
        }

        /* Tables */
        table{
          border-collapse: collapse;
          width: 100%;
          margin: 1rem 0 1.2rem;
          font-variant-numeric: tabular-nums;
        }
        th, td{
          padding: .55rem .7rem;
          border-bottom: 1px solid var(--border);
          text-align: left;
          vertical-align: top;
        }
        thead th{
          position: sticky;
          top: 0;
          background: #fafafa;
          z-index: 1;
        }
        tbody tr:nth-child(even){
          background: #fbfbfc;
        }

        /* Code (inline) */
        code{
          font-family: "DejaVu Sans Mono", "Liberation Mono", "Noto Sans Mono",
                       ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
          background: var(--code-bg);
          border: 1px solid #eef0f2;
          padding: 0.15em 0.35em;
          border-radius: 4px;
          font-weight: 700; /* bold inline code, as you wanted */
          font-size: 0.95em;
          color: #0d3a33; /* dark teal for contrast on Linux */
        }

        /* Code blocks */
        pre{
          margin: 1rem 0 1.2rem;
          padding: .9rem 1rem;
          background: var(--code-bg);
          border: 1px solid #eef0f2;
          border-radius: 6px;
          overflow: auto;
        }
        pre code{
          font-weight: 400;     /* keep block code regular for readability */
          color: #0f172a;       /* near-black */
          background: transparent;
          border: 0;
          padding: 0;
        }

        /* Pandoc highlight (Skylighting) tweaks */
        code span.kw { color:#7c3aed; font-weight: 600; } /* keyword */
        code span.st { color:#065f46; }                   /* string */
        code span.co { color:#6b7280; font-style: italic; } /* comment */
        code span.fu { color:#1d4ed8; }                   /* function */
        code span.op { color:#374151; }
        code span.bn, code span.dv, code span.fl { color:#b45309; } /* numbers */

        /* Horizontal rule */
        hr{
          border: 0;
          border-top: 1px solid var(--border);
          margin: 1.6rem 0;
        }

        /* Definition lists (Pandoc) */
        dl{ margin: 0 0 1rem; }
        dt{ font-weight: 600; }
        dd{ margin: 0 0 .8rem 1rem; }

        /* Small UI bits */
        kbd{
          font-family: inherit;
          background:#f3f4f6;
          border:1px solid #e5e7eb;
          border-bottom-width:2px;
          padding:.1rem .35rem;
          border-radius:4px;
          font-size:.9em;
        }

        /* MathJax sizing */
        .math.inline{ font-size: 0.98em; }
        .math.display{ font-size: 1.02em; }

        /* Footnotes / anchors spacing so anchor links land nicely */
        [id]::before{
          content: "";
          display: block;
          height: 0px; /* no fixed header, so no offset needed; keep for future */
          margin-top: 0;
          visibility: hidden;
        }

        /* Print-friendly */
        @media print{
          body{
            margin: 0;
            padding: 0 1cm;
            max-width: none;
            color: #000;
          }
          a[href]::after{ content: ""; } /* don’t print URLs */
          pre{ page-break-inside: avoid; }
          h2, h3{ page-break-after: avoid; }
        }
        """;

    webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
      if (newState == Worker.State.SUCCEEDED) {
        String script = """
                var style = document.createElement('style');
                style.type = 'text/css';
                style.appendChild(document.createTextNode(%s));
                document.head.appendChild(style);
            """.formatted(jsEscape(modernLightCss));

        webEngine.executeScript(script);
      }
    });

    // Handlers for zoom
    zoomInButton.setOnAction(e -> browser.setZoom(browser.getZoom() + 0.1));
    zoomOutButton.setOnAction(e -> browser.setZoom(browser.getZoom() - 0.1));
    resetZoomButton.setOnAction(e -> browser.setZoom(1.0)); // Reset to default zoom

    btnBox = new HBox(5, zoomInButton, zoomOutButton, resetZoomButton);
  }

  private static String jsEscape(String css) {
    return "\"" + css
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "") + "\"";
  }

  private void disableInternalWebViewDnD(WebView webView) {

    webView.addEventFilter(DragEvent.DRAG_OVER, event -> {
      if (event.getDragboard().hasFiles()) {
        // Accept so parent can process it
        event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
      }
      // DO NOT consume
    });

    webView.addEventFilter(DragEvent.DRAG_DROPPED, event -> {
      if (event.getDragboard().hasFiles()) {

        DragEvent copy = event.copyFor(
            webView.getParent(),
            webView.getParent()
        );

        webView.getParent().fireEvent(copy);

        event.setDropCompleted(false);
        event.consume();
      }
    });
  }


  @Override
  protected void layoutChildren() {
    double w = getWidth();
    double h = getHeight();
    layoutInArea(browser, 0, 0, w, h, 0, HPos.CENTER, VPos.CENTER);
  }

  @Override
  protected double computePrefWidth(double height) {
    return 900;
  }

  @Override
  protected double computePrefHeight(double width) {
    return 900;
  }

  public HBox getBtnBox() {
    return btnBox;
  }
}