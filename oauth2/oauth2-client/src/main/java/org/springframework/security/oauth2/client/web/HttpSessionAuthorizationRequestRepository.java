/*
 * Copyright 2012-2017 the original author or authors.
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
package org.springframework.security.oauth2.client.web;

import org.springframework.security.oauth2.core.endpoint.AuthorizationRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * An implementation of an {@link AuthorizationRequestRepository} that stores
 * {@link AuthorizationRequest} in the {@link HttpSession}.
 *
 * @author Joe Grandja
 * @since 5.0
 * @see AuthorizationRequest
 */
public final class HttpSessionAuthorizationRequestRepository implements AuthorizationRequestRepository {
	private static final String DEFAULT_AUTHORIZATION_REQUEST_ATTR_NAME =
			HttpSessionAuthorizationRequestRepository.class.getName() +  ".AUTHORIZATION_REQUEST";
	private final String sessionAttributeName = DEFAULT_AUTHORIZATION_REQUEST_ATTR_NAME;

	@Override
	public AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			return (AuthorizationRequest) session.getAttribute(this.sessionAttributeName);
		}
		return null;
	}

	@Override
	public void saveAuthorizationRequest(AuthorizationRequest authorizationRequest, HttpServletRequest request,
											HttpServletResponse response) {
		if (authorizationRequest == null) {
			this.removeAuthorizationRequest(request);
			return;
		}
		request.getSession().setAttribute(this.sessionAttributeName, authorizationRequest);
	}

	@Override
	public void removeAuthorizationRequest(HttpServletRequest request) {
		request.getSession().removeAttribute(this.sessionAttributeName);
	}
}
