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

import com.actelion.research.share.gui.editor.io.IMouseEvent;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Touch;
import com.google.gwt.event.dom.client.MouseEvent;
import com.google.gwt.event.dom.client.TouchEvent;

public class FakeMouseEvent implements IMouseEvent {
  private int x = 0;
  private int y = 0;
  private boolean shift = false;
  private boolean ctrl = false;
  private boolean alt = false;

  static MousePoint getPointFromTouchEvent(TouchEvent evt) {
    JsArray<Touch> touches = evt.getTouches();
    Touch touch = touches.get(0);
    if (touch == null) {
      return null;
    }
    Element target = evt.getRelativeElement();
    int x = touch.getRelativeX(target);
    int y = touch.getRelativeY(target);
    return new MousePoint(x, y);
  }

  public FakeMouseEvent(MouseEvent evt) {
    x = evt.getX();
    y = evt.getY();
    shift = evt.isShiftKeyDown();
    ctrl = evt.isControlKeyDown();
    alt = evt.isAltKeyDown();
  }

  public FakeMouseEvent(TouchEvent evt) {
    MousePoint point = getPointFromTouchEvent(evt);
    if (point != null) {
      x = point.getX();
      y = point.getY();
    }
    shift = evt.isShiftKeyDown();
    ctrl = evt.isControlKeyDown();
    alt = evt.isAltKeyDown();
  }

  public FakeMouseEvent(TouchEvent evt, MousePoint mousePoint) {
    x = mousePoint.getX();
    y = mousePoint.getY();
    shift = evt.isShiftKeyDown();
    ctrl = evt.isControlKeyDown();
    alt = evt.isAltKeyDown();
  }

  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }

  public boolean isShiftDown() {
    return shift;
  }

  public boolean isControlDown() {
    return ctrl;
  }

  @Override
  public boolean isAltDown() {
    return alt;
  }
}
