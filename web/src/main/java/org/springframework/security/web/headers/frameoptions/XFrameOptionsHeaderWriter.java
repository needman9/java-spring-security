/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.security.web.headers.frameoptions;

import org.springframework.security.web.headers.HeaderWriter;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * {@code HeaderWriter} implementation for the X-Frame-Options headers. When using the ALLOW-FROM directive the actual
 * value is determined by a {@code AllowFromStrategy}.
 *
 * @author Marten Deinum
 * @author Rob Winch
 * @since 3.2
 *
 * @see AllowFromStrategy
 */
public class XFrameOptionsHeaderWriter implements HeaderWriter {

    public static final String XFRAME_OPTIONS_HEADER = "X-Frame-Options";

    private final AllowFromStrategy allowFromStrategy;
    private final XFrameOptionsMode frameOptionsMode;

    /**
     * Creates a new instance
     *
     * @param frameOptionsMode
     *            the {@link XFrameOptionsMode} to use. If using
     *            {@link XFrameOptionsMode#ALLOW_FROM}, use
     *            {@link #FrameOptionsHeaderWriter(AllowFromStrategy)}
     *            instead.
     */
    public XFrameOptionsHeaderWriter(XFrameOptionsMode frameOptionsMode) {
        Assert.notNull(frameOptionsMode, "frameOptionsMode cannot be null");
        if(XFrameOptionsMode.ALLOW_FROM.equals(frameOptionsMode)) {
            throw new IllegalArgumentException(
                    "ALLOW_FROM requires an AllowFromStrategy. Please use FrameOptionsHeaderWriter(AllowFromStrategy allowFromStrategy) instead");
        }
        this.frameOptionsMode = frameOptionsMode;
        this.allowFromStrategy = null;
    }

    /**
     * Creates a new instance with {@link XFrameOptionsMode#ALLOW_FROM}.
     *
     * @param allowFromStrategy
     *            the strategy for determining what the value for ALLOW_FROM is.
     */
    public XFrameOptionsHeaderWriter(AllowFromStrategy allowFromStrategy) {
        Assert.notNull(allowFromStrategy, "allowFromStrategy cannot be null");
        this.frameOptionsMode = XFrameOptionsMode.ALLOW_FROM;
        this.allowFromStrategy = allowFromStrategy;
    }

    public void writeHeaders(HttpServletRequest request, HttpServletResponse response) {
        if (XFrameOptionsMode.ALLOW_FROM.equals(frameOptionsMode)) {
            String allowFromValue = allowFromStrategy.getAllowFromValue(request);
            if(allowFromValue != null) {
                response.addHeader(XFRAME_OPTIONS_HEADER, XFrameOptionsMode.ALLOW_FROM.getMode() + " " + allowFromValue);
            }
        } else {
            response.addHeader(XFRAME_OPTIONS_HEADER, frameOptionsMode.getMode());
        }
    }

    /**
     * The possible values for the X-Frame-Options header.
     *
     * @author Rob Winch
     * @since 3.2
     */
    public enum XFrameOptionsMode {
        DENY("DENY"),
        SAMEORIGIN("SAMEORIGIN"),
        ALLOW_FROM("ALLOW-FROM");

        private String mode;

        private XFrameOptionsMode(String mode) {
            this.mode = mode;
        }

        /**
         * Gets the mode for the X-Frame-Options header value. For example,
         * DENY, SAMEORIGIN, ALLOW-FROM. Cannot be null.
         *
         * @return the mode for the X-Frame-Options header value.
         */
        public String getMode() {
            return mode;
        }
    }
}
