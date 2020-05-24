package org.correomqtt.business.keyring.osxkeychain;

import net.east301.keyring.BackendNotSupportedException;
import net.east301.keyring.PasswordRetrievalException;
import net.east301.keyring.PasswordSaveException;
import net.east301.keyring.osx.OSXKeychainBackend;
import org.correomqtt.business.keyring.KeyringException;
import org.correomqtt.plugin.spi.KeyringHook;
import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Extension
public class OSXKeychainKeyring implements KeyringHook {

    private static final Logger LOGGER = LoggerFactory.getLogger(OSXKeychainKeyring.class);
    private static final String SERVICE_NAME = "CorreoMQTT";

    @Override
    public String getPassword(String label) {
        OSXKeychainBackend keychainBackend = new OSXKeychainBackend();
        try {
            keychainBackend.setup();
            return keychainBackend.getPassword(SERVICE_NAME, label);
        } catch (PasswordRetrievalException | BackendNotSupportedException e) {
            LOGGER.error("Failed to retrieve password from osx keychain.", e);
            throw new KeyringException("Failed to retrieve password from osx keychain.", e);
        }
    }

    @Override
    public void setPassword(String label, String password) {
        OSXKeychainBackend keychainBackend = new OSXKeychainBackend();
        try {
            keychainBackend.setup();
            keychainBackend.setPassword(SERVICE_NAME, label, password);
        } catch (BackendNotSupportedException | PasswordSaveException e) {
            LOGGER.error("Failed to retrieve password from osx keychain.", e);
            throw new KeyringException("Failed to retrieve password from osx keychain.", e);
        }
    }

    @Override
    public boolean isSupported() {
        return new OSXKeychainBackend().isSupported();
    }

    @Override
    public String getIdentifier() {
        return "OSXKeychain";
    }
}
