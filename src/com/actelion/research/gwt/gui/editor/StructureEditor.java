/*

Copyright (c) 2015-2017, cheminfo

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of {{ project }} nor the names of its contributors
      may be used to endorse or promote products derived from this software
      without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.actelion.research.gwt.gui.editor;

import com.actelion.research.chem.AbstractDepictor;
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.MolfileParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.gwt.gui.viewer.Log;
import com.actelion.research.share.gui.editor.Model;
import com.actelion.research.share.gui.editor.actions.Action;
import com.actelion.research.share.gui.editor.actions.NewChainAction;
import com.actelion.research.share.gui.editor.actions.SelectionAction;
import com.actelion.research.share.gui.editor.actions.ZoomRotateAction;
import com.actelion.research.share.gui.editor.geom.ICursor;
import com.actelion.research.share.gui.editor.io.IKeyEvent;
import com.actelion.research.share.gui.editor.listeners.IChangeListener;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.*;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import jsinterop.annotations.*;
import com.actelion.research.gwt.minimal.JSMolecule;
import java.util.ArrayList;
import java.util.List;

/**
 * Project: User: rufenec Date: 7/1/2014 Time: 10:05 AM
 */
@JsType(namespace = "OCL")
public class StructureEditor implements IChangeListener {
  static int TEXTHEIGHT = 20;
  static int TOOLBARWIDTH = 45;

  private boolean drag = false;
  private MousePoint mousePoint = null;
  private Model model;
  private ToolBar<Element> toolBar;
  private DrawArea drawPane;
  private InputElement idcodeTextElement;
  private LabelElement fragmentStatus;
  private Element container = null;
  private Boolean viewOnly = false;
  private static List<StructureEditor> map = new ArrayList<StructureEditor>();

  private boolean rightClick = false;
  private FakeMouseEvent mousePressEvt = null;

  private int scale = 1;
  static {
    initObserver();
  }

  @JsIgnore
  public StructureEditor() {
    this("editor", false, 1);
  }

  @JsIgnore
  public StructureEditor(String id) {
    this(id, false, 1);
  }

  @JsIgnore
  public StructureEditor(String id, boolean useSVG, int scale) {
    this(Document.get().getElementById(id), useSVG, scale);
  }

  public native Object getOCL()
  /*-{
    return $wnd.OCL;
  }-*/;

  public StructureEditor(Element container, boolean useSVG, int scale) {
    this.scale = scale;
    this.container = container;

    if (container != null) {
      model = new GWTEditorModel(new GWTGeomFactory(new GWTDrawConfig()), 0);
      String vo = container.getAttribute("view-only");
      viewOnly = Boolean.parseBoolean(vo);

      // Todo: Implement the following options:

      String se = container.getAttribute("ignore-stereo-errors");
      boolean ignoreStereoErrors = Boolean.parseBoolean(se);

      String st = container.getAttribute("no-stereo-text");
      boolean noStereoText = Boolean.parseBoolean(st);

      String sf = container.getAttribute("show-fragment-indicator");
      final boolean showFragmentIndicator = Boolean.parseBoolean(sf);

      // End Todo

      String si = container.getAttribute("show-idcode");
      boolean showIDCode = Boolean.parseBoolean(si);

      String isfragment = container.getAttribute("is-fragment");
      boolean isFragment = Boolean.parseBoolean(isfragment);

      String style = container.getAttribute("style");
      style += ";overflow:hidden;position:relative;";
      container.setAttribute("style", style);

      final int width = container.getClientWidth();
      final int height = container.getClientHeight();
      String idcode = container.getAttribute("data-idcode");

      final int toolBarWidth = getToolbarWidth();
      if (useSVG)
        toolBar = new SVGToolBarImpl(model, scale);
      else
        toolBar = new ToolBarImpl(model);
      Element toolBarElement = toolBar.createElement(container, toolBarWidth, height - TEXTHEIGHT - 5);
      if (!viewOnly)
        container.appendChild(toolBarElement);

      drawPane = new DrawArea(model);
      Element drawAreaElement = drawPane.createElement(container, toolBarWidth, 0, (width - toolBarWidth),
          height - TEXTHEIGHT - 5);
      container.appendChild(drawAreaElement);

      int idcodewidth = showFragmentIndicator ? width - 50 : width;
      if (showFragmentIndicator) {
        fragmentStatus = Document.get().createLabelElement();
        setElementSizePos(fragmentStatus, width - 50, height - TEXTHEIGHT - 5, TEXTHEIGHT, 50);
        String t = fragmentStatus.getAttribute("style");
        fragmentStatus.setAttribute("style", t + "text-align:center;");
        fragmentStatus.setInnerText(model.isFragment() ? "Q" : "");
        container.appendChild(fragmentStatus);
      }
      if (showIDCode) {
        idcodeTextElement = Document.get().createTextInputElement();
        setElementSizePos(idcodeTextElement, 0, height - TEXTHEIGHT - 5, TEXTHEIGHT, idcodewidth);
        container.appendChild(idcodeTextElement);
      }

      int displayMode = model.getDisplayMode();
      if (ignoreStereoErrors)
        displayMode |= AbstractDepictor.cDModeNoStereoProblem;

      if (noStereoText)
        displayMode |= AbstractDepictor.cDModeSuppressChiralText;

      model.setDisplayMode(displayMode);
      model.addChangeListener(this);
      StereoMolecule mol = createMolecule(idcode, isFragment/* , (width - toolBarWidth), height - TEXTHEIGHT - 5 */);
      model.setValue(mol, true);

      setUpHandlers();
      setupMouseHandlers();

      // Respond to resizing
      com.google.gwt.user.client.Window.addResizeHandler(new ResizeHandler() {
        public void onResize(ResizeEvent ev) {
          int w = container.getClientWidth();
          int h = container.getClientHeight();
          int idcodewidth = showFragmentIndicator ? w - 30 : h;
          if (width != w || height != h) {
            drawPane.setSize(w - toolBarWidth, h - TEXTHEIGHT - 5);
            if (idcodeTextElement != null)
              setElementSizePos(idcodeTextElement, 0, h - TEXTHEIGHT - 5, TEXTHEIGHT, idcodewidth);
            if (fragmentStatus != null)
              setElementSizePos(fragmentStatus, w - 30, h - TEXTHEIGHT - 5, TEXTHEIGHT, 30);
          }
        }
      });
    }
    addPasteHandler(this, "onMouseClicked ");
  }

  public static StructureEditor createEditor(String id) {
    return new StructureEditor(id);
  }

  public static StructureEditor createSVGEditor(String id, int scale) {
    return new StructureEditor(id, true, scale);
  }

  private native static void initObserver()
  /*-{
      if ('MutationObserver' in $wnd) {
          $wnd.edit$observer = new $wnd.MutationObserver(function (mutations) {
              mutations.forEach(function (mutation) {
                  @com.actelion.research.gwt.gui.editor.StructureEditor::notify(Ljava/lang/Object;)(mutation.target);
              });
          });
      }
  }-*/;

  public static void notify(Object o) {
    for (StructureEditor e : map) {
      e.drawPane.draw();
      int w = e.container.getClientWidth();
      int h = e.container.getClientHeight();
      final int toolBarWidth = e.getToolbarWidth();
      e.drawPane.setSize(w - toolBarWidth, h - TEXTHEIGHT - 5);
    }

  }

  private int getToolbarWidth() {
    return viewOnly ? 0 : TOOLBARWIDTH * scale;
  }

  private void setElementSizePos(Element el, int x, int y, int h, int w) {
    el.setAttribute("style", "position:absolute;" + "left:" + x + "px; " + "top:" + y + "px;" + "width:" + w + "px;"
        + "height:" + h + "px;");
  }

  public JSMolecule getMolecule() {
    return new JSMolecule(model.getMolecule());
  }

  public String getIDCode() {
    return model.getIDCode();
  }

  public void setIDCode(String idCode) {
    try {
      StereoMolecule mol = new StereoMolecule();
      IDCodeParser p = new IDCodeParser();
      if (idCode != null) {
        String[] parts = idCode.split(" ");
        if (parts.length > 1) {
          p.parse(mol, parts[0], parts[1]);
        } else
          p.parse(mol, idCode);
        model.setValue(mol, true);
      }
    } catch (Exception e) {
      Log.println("error setting idcode data " + e);
    } finally {
    }
  }

  public void setFragment(boolean set) {
    model.setFragment(set);
  }

  public boolean isFragment() {
    return model.isFragment();
  }

  public String getMolFile() {
    return model.getMolFile(false);
  }

  public void setMolFile(String molFile) {
    model.setMolFile(molFile);
  }

  public String getMolFileV3() {
    return model.getMolFile(true);
  }

  public String getSmiles() {
    return model.getSmiles();
  }

  public void setSmiles(String smiles) {
    model.setSmiles(smiles);
  }

  public void setAtomHightlightCallback(final JavaScriptObject atomHightlightCallback) {
    if (atomHightlightCallback != null) {
      model.registerAtomHighlightCallback(new Model.AtomHighlightCallback() {
        @Override
        public void onHighlight(int atom, boolean selected) {
          callFuncIZ(atomHightlightCallback, atom, selected);
        }
      });
    } else
      model.registerAtomHighlightCallback(null);
  }

  public void setBondHightlightCallback(final JavaScriptObject bondHightlightCallback) {
    if (bondHightlightCallback != null) {
      model.registerBondHighlightCallback(new Model.BondHighlightCallback() {
        @Override
        public void onHighlight(int bond, boolean selected) {
          callFuncIZ(bondHightlightCallback, bond, selected);
        }
      });
    } else
      model.registerBondHighlightCallback(null);
  }

  public void setChangeListenerCallback(final JavaScriptObject cb) {
    if (cb != null) {
      model.addChangeListener(new IChangeListener() {
        @Override
        public void onChange() {
          String idcode = model.getIDCode();
          callFuncS(cb, idcode, getMolecule());

        }
      });
    }
  }

  private StereoMolecule createMolecule(String idcode, boolean fragment/* , int width, int height */) {
    StereoMolecule mol = new StereoMolecule();
    mol.setFragment(fragment);
    if (idcode != null && idcode.trim().length() > 0) {
      IDCodeParser p = new IDCodeParser();
      String[] elements = idcode.split(" ");
      if (elements == null || elements.length == 1)
        p.parse(mol, idcode);
      else
        p.parse(mol, elements[0], elements[1]);

      mol.setStereoBondsFromParity();
      /*
       * GWTDepictor depictor = new GWTDepictor(mol); depictor.updateCoords(null, new
       * java.awt.geom.Rectangle2D.Double(0, 0, (float) width, (float) height),
       * GWTDepictor.cModeInflateToMaxAVBL );
       */

    }
    return mol;
  }

  private void setUpHandlers() {
  }

  private Context2d getContext2d() {
    return drawPane.canvas.getContext2d();
  }

  private void setupMouseHandlers() {
    drawPane.setOnMouseClicked(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        onMouseClicked(event, false);
      }
    }, new DoubleClickHandler() {
      @Override
      public void onDoubleClick(DoubleClickEvent event) {
        onMouseClicked(event, true);

      }
    });

    drawPane.setOnMouseDragged(new MouseMoveHandler() {
      @Override
      public void onMouseMove(MouseMoveEvent event) {
        if (drag)
          onMouseDragged(event);

      }
    });
    drawPane.setOnMouseOut(new MouseOutHandler() {
      @Override
      public void onMouseOut(MouseOutEvent event) {
        mousePoint = null;
      }
    });
    drawPane.setOnMouseDown(new MouseDownHandler() {
      @Override
      public void onMouseDown(MouseDownEvent event) {
        onMousePressed(event);
      }
    });
    drawPane.setOnTouchStart(new TouchStartHandler() {
      @Override
      public void onTouchStart(TouchStartEvent event) {
         onTouchStarted(event);
      }
    });
    drawPane.setOnMouseMove(new MouseMoveHandler() {
      @Override
      public void onMouseMove(MouseMoveEvent event) {
        boolean moved = mousePoint == null ? false : true;
        if (!drag && moved) {
          onMouseMoved(event);
        }
        mousePoint = new MousePoint(event.getX(), event.getY());
      }
    });
    drawPane.setOnTouchMove(new TouchMoveHandler() {
      @Override
      public void onTouchMove(TouchMoveEvent event) {
        if (drag) {
          onTouchMoved(event);
        }
      }
    });
    drawPane.setOnTouchCancel(new TouchCancelHandler() {
      @Override
      public void onTouchCancel(TouchCancelEvent event) {
        onTouchCancelled(event);
      }
    });
    drawPane.setOnMouseUp(new MouseUpHandler() {
      @Override
      public void onMouseUp(MouseUpEvent event) {
        onMouseReleased(event);
      }
    });
    drawPane.setOnTouchEnd(new TouchEndHandler() {
      @Override
      public void onTouchEnd(TouchEndEvent event) {
        // Simulate a previous mouse move event so action that depend on the currently hovered element can work properly.
        // Touch screens only emit move events for some hardware like the Apple Pencil Pro.
        onMouseMovedFake(event);
        onTouchEnded(event);
      }
    });
    drawPane.setOnKeyPressed(new ACTKeyEventHandler() {
      @Override
      public void onKey(IKeyEvent event) {
        onKeyPressed(event);
      }
    });

    /*
     * drawPane.setOnKeyReleased(new ACTKeyEventHandler() {
     *
     * @Override public void onKey(IKeyEvent event) { onKeyPressed(event); } });
     */

  }

  private void onKeyPressed(IKeyEvent keyEvent) {
    Action a = toolBar.getCurrentAction();
    if (a != null) {
      if (a.onKeyPressed(keyEvent)) {
        model.changed();
      } else {
        handleKeyEvent(keyEvent);
      }
    }
  }

  private void handleKeyEvent(IKeyEvent keyEvent) {
  }

  public boolean onPasteString(String s) {
    try {
      StereoMolecule mol = new StereoMolecule();
      MolfileParser parser = new MolfileParser();
      parser.parse(mol, s);
      model.addMolecule(mol, 0, 0);
      return true;
    } catch (Exception e) {
      Log.console("Parse exception " + e);
    }
    try {
      StereoMolecule mol = new StereoMolecule();
      IDCodeParser p = new IDCodeParser(true);
      p.parse(mol, s);
      model.addMolecule(mol, 0, 0);
      return true;
    } catch (Exception e) {
      Log.console("Parse exception " + e);
    }
    return false;
  }

  public boolean onPasteImage(Object s) {
    return false;
  }

  public boolean hasFocus() {
    return drawPane.hasFocus() || toolBar.hasFocus();
  }

  public static native void addPasteHandler(StructureEditor self, String text)
  /*-{
      $doc.addEventListener('paste', function (e) {
          var foxus = self.@com.actelion.research.gwt.gui.editor.StructureEditor::hasFocus()();
          if (foxus && e.clipboardData) {
              var items = e.clipboardData.items;
              var done = false;
              if (items) {
                  //access data directly
                  for (var i = 0; !done && i < items.length; i++) {
                      console.log("Item is " + items[i].type);
                      if (items[i].type.indexOf("image") !== -1) {
                          //image
                          var blob = items[i].getAsFile();
                          var URLObj = window.URL || window.webkitURL;
                          var source = URLObj.createObjectURL(blob);

                          var pastedImage = new Image();
                          pastedImage.onload = function () {
                              if(self.@com.actelion.research.gwt.gui.editor.StructureEditor::onPasteImage(Ljava/lang/Object;)(pastedImage)) {
                                  done = true;
                              }
                          };
                          pastedImage.src = source;
                      } else if (items[i].type.indexOf("text/plain") !== -1) {
                          items[i].getAsString(function (s){
                             if (self.@com.actelion.research.gwt.gui.editor.StructureEditor::onPasteString(Ljava/lang/String;)(s)) {
                                 done = true;
                             }
                          });

                      }
                  }
                  e.preventDefault();
              }
          }

      }, false); //official paste handler
  }-*/;

  private void onMouseClicked(MouseEvent evt, boolean dbl) {
    if (!rightClick) {
      Action a = toolBar.getCurrentAction();
      if (a != null && !a.isCommand()) {
        if (dbl && a.onDoubleClick(new ACTMouseEvent(evt))) {
          drawPane.draw(a);
        }
      }
    }

  }

  private void onMousePressed(MouseEvent evt) {
    drag = true;
    mousePressEvt = new FakeMouseEvent(evt);
    if (evt.getNativeButton() == NativeEvent.BUTTON_RIGHT) {
      rightClick = true;
    }
    if (!rightClick) {
      Action a = toolBar.getCurrentAction();
      if (a != null && !a.isCommand()) {
        a.onMouseDown(new ACTMouseEvent(evt));
        setCursor(a.getCursor());
      }
    }
  }

  private void onTouchStarted(TouchEvent evt) {
    evt.preventDefault();
    mousePoint = FakeMouseEvent.getPointFromTouchEvent(evt);
    drag = true;
    mousePressEvt = new FakeMouseEvent(evt);
    Action a = toolBar.getCurrentAction();
    if (a != null && !a.isCommand()) {
      a.onMouseDown(new ACTTouchEvent(evt, mousePoint));
      setCursor(a.getCursor());
    }
  }

  private void onMouseMoved(MouseEvent evt) {
    if (!rightClick) {
      Action a = toolBar.getCurrentAction();
      if (a != null && !a.isCommand()) {
        if (a.onMouseMove(new ACTMouseEvent(evt), false)) {
          drawPane.draw(a);
          drawPane.requestFocus();
        }
      }
    }
  }

  private void onMouseMovedFake(TouchEvent evt) {
    Action a = toolBar.getCurrentAction();
    if (mousePoint != null && a != null && !a.isCommand()) {
      if (a.onMouseMove(new ACTTouchEvent(evt, mousePoint), false)) {
        drawPane.draw(a);
      }
    }
  }

  private void onMouseDragged(MouseEvent evt) {
    if (!rightClick) {
      Action a = toolBar.getCurrentAction();
      if (a != null && !a.isCommand()) {
        FakeMouseEvent thisEvt = new FakeMouseEvent(evt);
        if (mousePressEvt == null || !isSmallMovement(thisEvt, mousePressEvt)) {
          if (a.onMouseMove(new ACTMouseEvent(evt), true)) {
            drawPane.draw(a);
          }
        }
      }
    }
  }

  private void onTouchMoved(TouchEvent evt) {
    evt.preventDefault();
    mousePoint = FakeMouseEvent.getPointFromTouchEvent(evt);
    Action a = toolBar.getCurrentAction();
    if (a != null && !a.isCommand()) {
      FakeMouseEvent thisEvt = new FakeMouseEvent(evt);
      if (mousePressEvt == null || !isSmallMovement(thisEvt, mousePressEvt)) {
        if (a.onMouseMove(new ACTTouchEvent(evt, mousePoint), true)) {
          drawPane.draw(a);
        }
      }
    }
  }

  private void onTouchCancelled(TouchEvent evt) {
    drag = false;
    mousePressEvt = null;
  }

  private void onMouseReleased(MouseEvent evt) {
    drag = false;
    if (!rightClick) {
      Action a = toolBar.getCurrentAction();
      if (a != null && !a.isCommand()) {
        FakeMouseEvent thisEvt = new FakeMouseEvent(evt);
        FakeMouseEvent syntheticEvt = null;
        if (mousePressEvt != null && isSmallMovement(thisEvt, mousePressEvt)) {
          syntheticEvt = mousePressEvt;
        } else {
          syntheticEvt = thisEvt;
        }
        if (a.onMouseUp(syntheticEvt)) {
          drawPane.draw();
          model.changed();
        }
      }
    }
    setCursor(ICursor.DEFAULT);
    rightClick = false;
    mousePressEvt = null;
    mousePoint = null;
  }

  private void onTouchEnded(TouchEvent evt) {
    evt.preventDefault();
    drag = false;
    Action a = toolBar.getCurrentAction();
    if (a != null && !a.isCommand()) {
      FakeMouseEvent thisEvt = new FakeMouseEvent(evt, mousePoint);
      FakeMouseEvent syntheticEvt = null;
      if (mousePressEvt != null && isSmallMovement(thisEvt, mousePressEvt)) {
        syntheticEvt = mousePressEvt;
      } else {
        syntheticEvt = thisEvt;
      }
      if (a.onMouseUp(syntheticEvt)) {
        drawPane.draw();
        model.changed();
      }
    }
    setCursor(ICursor.DEFAULT);
    rightClick = false;
    mousePressEvt = null;
    mousePoint = null;
  }

  private boolean isSmallMovement(FakeMouseEvent evt1, FakeMouseEvent evt2) {
    double distance = Math.pow(evt1.getX() - evt2.getX(), 2) + Math.pow(evt1.getY() - evt2.getY(), 2);
    return distance <= 36.0;
  }

  private void setCursor(int c) {
    switch (c) {
    case ICursor.SE_RESIZE:
      drawPane.setCursor(Style.Cursor.SE_RESIZE);
      break;
    case ICursor.CROSSHAIR:
      drawPane.setCursor(Style.Cursor.CROSSHAIR);
      break;
    case ICursor.DEFAULT:
    default:
      drawPane.setCursor(Style.Cursor.DEFAULT);
      break;
    }
  }

  private void doAction(Action a) {
    toolBar.doAction(a);
    drawPane.requestLayout();
  }

  @Override
  public void onChange() {
    if (idcodeTextElement != null) {
      String idcode = model.getIDCode();
      idcodeTextElement.setValue(idcode);
    }
    if (fragmentStatus != null) {
      fragmentStatus.setInnerText(model.isFragment() ? "Q" : "");
    }
  }

  private native void callFuncIZ(JavaScriptObject func, int param, boolean b) /*-{
                                                                              func(param, b);
                                                                              }-*/;

  private native void callFuncS(JavaScriptObject func, String f, JSMolecule jsMolecule) /*-{
                                                                                        func(f, jsMolecule);
                                                                                        }-*/;

}
