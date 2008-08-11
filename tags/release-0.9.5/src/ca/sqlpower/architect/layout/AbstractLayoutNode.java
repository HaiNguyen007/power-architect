/*
 * Copyright (c) 2007, SQL Power Group Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of SQL Power Group Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ca.sqlpower.architect.layout;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

/**
 * Provides all the redundant features of the LayoutNode interface.
 * You only have to implement the {@link #getBounds(Rectangle)},
 * {@link #getInboundEdges()}, and {@link #getOutboundEdges()},
 * {@link #getNodeName()}, and {@link #setBounds(int, int, int, int)}
 * methods.
 */
public abstract class AbstractLayoutNode implements LayoutNode {

    public abstract Rectangle getBounds(Rectangle b);
    public abstract List<LayoutEdge> getInboundEdges();
    public abstract List<LayoutEdge> getOutboundEdges();
    public abstract String getNodeName();
    public abstract void setBounds(int x, int i, int width, int height);

    public Rectangle getBounds() {
        return getBounds(new Rectangle());
    }

    public int getX() {
        return getBounds().x;
    }

    public int getY() {
        return getBounds().y;
    }

    public int getWidth() {
        return getBounds().width;
    }

    public int getHeight() {
        return getBounds().height;
    }

    public Point getLocation() {
        Rectangle bounds = getBounds();
        return new Point(bounds.x, bounds.y);
    }

    public void setLocation(int x, int y) {
        Rectangle bounds = getBounds();
        setBounds(x, y, bounds.width, bounds.height);
    }

    public void setLocation(Point pos) {
        Rectangle bounds = getBounds();
        setBounds(pos.x, pos.y, bounds.width, bounds.height);
    }

}