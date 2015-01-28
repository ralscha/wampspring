/**
 * Copyright 2014-2015 Ralph Schaer <ralphschaer@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.rasc.wampspring.cra;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import ch.rasc.wampspring.config.WampSession;
import ch.rasc.wampspring.message.CallMessage;

public class DefaultAuthenticationHandler implements AuthenticationHandler {

	private final AuthenticationSecretProvider authenticationSecretProvider;

	public DefaultAuthenticationHandler(AuthenticationSecretProvider provider) {
		this.authenticationSecretProvider = provider;
	}

	@Override
	public Object handleAuthReq(String authKey, Map<String, Object> extra,
			CallMessage message) {

		WampSession wampSession = message.getWampSession();

		if (wampSession.isAuthRequested()) {
			throw new IllegalStateException("Already authenticated");
		}

		if (this.authenticationSecretProvider.getSecret(authKey) == null) {
			throw new IllegalStateException("Secret key does not exist");
		}

		try {
			final String challenge = generateHMacSHA256(message.getWebSocketSessionId()
					+ System.currentTimeMillis(), authKey);
			wampSession.setAuthKey(authKey);
			wampSession.setChallenge(challenge);
			return challenge;
		}
		catch (InvalidKeyException | NoSuchAlgorithmException e) {
			throw new IllegalStateException("invalid key", e);
		}
	}

	@Override
	public Object handleAuth(String clientSignature, CallMessage message) {
		WampSession wampSession = message.getWampSession();

		if (!wampSession.isAuthRequested()) {
			throw new IllegalStateException("No authentication previously requested");
		}

		final String correctSignature;
		try {
			final String secret = this.authenticationSecretProvider.getSecret(wampSession
					.getAuthKey());
			if (!StringUtils.hasText(secret)) {
				throw new IllegalStateException("Secret does not exist");
			}
			correctSignature = generateHMacSHA256(secret, wampSession.getChallenge());
		}
		catch (InvalidKeyException | NoSuchAlgorithmException e) {
			throw new IllegalStateException("invalid key", e);
		}

		if (clientSignature.equals(correctSignature)) {
			wampSession.setSignature(clientSignature);
			return null;
		}

		wampSession.setAuthKey(null);
		wampSession.setChallenge(null);
		wampSession.setSignature(null);
		throw new SecurityException("Signature for authentication request is invalid");
	}

	public static String generateHMacSHA256(final String key, final String data)
			throws InvalidKeyException, NoSuchAlgorithmException {
		Assert.notNull(key, "key is required");
		Assert.notNull(data, "data is required");

		final Mac hMacSHA256 = Mac.getInstance("HmacSHA256");
		byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
		final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA256");
		hMacSHA256.init(secretKey);
		byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
		byte[] res = hMacSHA256.doFinal(dataBytes);

		return DatatypeConverter.printBase64Binary(res);
	}

}
