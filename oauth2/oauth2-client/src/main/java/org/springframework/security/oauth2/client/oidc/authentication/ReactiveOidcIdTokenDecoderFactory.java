/*
 * Copyright 2002-2019 the original author or authors.
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
package org.springframework.security.oauth2.client.oidc.authentication;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoderFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Provides a default or custom reactive implementation for {@link OAuth2TokenValidator}
 *
 * @author Joe Grandja
 * @author Rafael Dominguez
 * @since 5.2
 *
 * @see OAuth2TokenValidator
 * @see Jwt
 */
public final class ReactiveOidcIdTokenDecoderFactory implements ReactiveJwtDecoderFactory<ClientRegistration> {
	private static final String MISSING_SIGNATURE_VERIFIER_ERROR_CODE = "missing_signature_verifier";
	private final Map<String, ReactiveJwtDecoder> jwtDecoders = new ConcurrentHashMap<>();
	private Function<ClientRegistration, OAuth2TokenValidator<Jwt>> jwtValidatorFactory = OidcIdTokenValidator::new;

	@Override
	public ReactiveJwtDecoder createDecoder(ClientRegistration clientRegistration) {
		Assert.notNull(clientRegistration, "clientRegistration cannot be null.");
		return this.jwtDecoders.computeIfAbsent(clientRegistration.getRegistrationId(), key -> {
			if (!StringUtils.hasText(clientRegistration.getProviderDetails().getJwkSetUri())) {
				OAuth2Error oauth2Error = new OAuth2Error(
						MISSING_SIGNATURE_VERIFIER_ERROR_CODE,
						"Failed to find a Signature Verifier for Client Registration: '" +
								clientRegistration.getRegistrationId() +
								"'. Check to ensure you have configured the JwkSet URI.",
						null
				);
				throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
			}
			NimbusReactiveJwtDecoder jwtDecoder = new NimbusReactiveJwtDecoder(
					clientRegistration.getProviderDetails().getJwkSetUri());
			OAuth2TokenValidator<Jwt> jwtValidator = jwtValidatorFactory.apply(clientRegistration);
			jwtDecoder.setJwtValidator(jwtValidator);
			return jwtDecoder;
		});
	}

	/**
	 * Allows user customization for the {@link OAuth2TokenValidator}
	 *
	 * @param jwtValidatorFactory
	 */
	public final void setJwtValidatorFactory(Function<ClientRegistration, OAuth2TokenValidator<Jwt>> jwtValidatorFactory) {
		Assert.notNull(jwtValidatorFactory, "jwtValidatorFactory cannot be null.");
		this.jwtValidatorFactory = jwtValidatorFactory;
	}
}
