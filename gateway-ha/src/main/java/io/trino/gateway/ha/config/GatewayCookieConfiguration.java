/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.gateway.ha.config;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import static java.nio.charset.StandardCharsets.UTF_8;

public class GatewayCookieConfiguration
{
    private SecretKey cookieSigningKey;
    private boolean enabled;

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public SecretKey getCookieSigningKey()
    {
        return cookieSigningKey;
    }

    public void setCookieSigningSecret(String cookieSigningSecret)
    {
        cookieSigningKey = new SecretKeySpec(cookieSigningSecret.getBytes(UTF_8), "HmacSHA256");
    }
}
