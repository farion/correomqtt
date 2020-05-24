package org.correomqtt.business.keyring.windpapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.windpapi4j.InitializationFailedException;
import com.github.windpapi4j.WinAPICallFailedException;
import com.github.windpapi4j.WinDPAPI;
import org.correomqtt.business.keyring.KeyringException;
import org.correomqtt.business.provider.SettingsProvider;
import org.correomqtt.plugin.spi.KeyringHook;
import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Extension
public class WinDPAPIKeyring implements KeyringHook {

    private static final Logger LOGGER = LoggerFactory.getLogger(WinDPAPIKeyring.class);

    private static final Charset STD_CHAR_SET = StandardCharsets.UTF_8;

    @Override
    public boolean requiresUserinput() {
        return false;
    }

    @Override
    public String getPassword(String label) {
        Map<String, String> data = readData();
        return data.get(label);
    }

    @Override
    public void setPassword(String label, String password) {
        Map<String, String> data = readData();
        data.put(label,password);
        writeData(data);
    }

    private void writeData(Map<String, String> data) {
        try {
            String protectedData = protect(data);
            File file = getFile();
            new ObjectMapper().writeValue(file, WinDPAPIKeyringDTO.builder().data(protectedData).build());
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to write json data for WinDPAPI.", e);
            throw new KeyringException("Failed to write json data for WinDPAPI.", e);
        } catch (IOException e) {
            LOGGER.error("Failed to write file with data from WinDPAPI.", e);
            throw new KeyringException("Failed to write file with data from WinDPAPI.", e);
        }
    }

    private String protect(Map<String, String> data) {
        try {
            String unprotectedData = new ObjectMapper().writeValueAsString(data);
            WinDPAPI winDPAPI = WinDPAPI.newInstance(WinDPAPI.CryptProtectFlag.CRYPTPROTECT_UI_FORBIDDEN);
            return new String(winDPAPI.protectData(unprotectedData.getBytes(STD_CHAR_SET)), STD_CHAR_SET);
        } catch (InitializationFailedException | WinAPICallFailedException e) {
            LOGGER.error("Failed to unprotect data with WinDPAPI.", e);
            throw new KeyringException("Failed to unprotect data with WinDPAPI.", e);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to parse data from WinDPAPI.", e);
            throw new KeyringException("Failed to parse data from WinDPAPI.", e);
        }

    }

    private Map<String,String> readData(){
        File file = getFile();
        if(file.exists()) {
            try {
                WinDPAPIKeyringDTO winDPAPIKeyringDTO = new ObjectMapper().readValue(file, WinDPAPIKeyringDTO.class);
                String data = winDPAPIKeyringDTO.getData();
                return unprotect(data);
            }catch(IOException e){
                LOGGER.error("Reading WinDPAPI file failed.", e);
                throw new KeyringException("Reading WinDPAPI file failed.", e);
            }
        }else{
            return new HashMap<>();
        }
    }

    private Map<String, String> unprotect(String protectedData) {
        try {
            WinDPAPI winDPAPI = WinDPAPI.newInstance(WinDPAPI.CryptProtectFlag.CRYPTPROTECT_UI_FORBIDDEN);
            String unprotectedData = new String(winDPAPI.unprotectData(protectedData.getBytes(STD_CHAR_SET)),STD_CHAR_SET);
            return new ObjectMapper().readValue(unprotectedData,new TypeReference<HashMap<String, String>>() {
            });
        } catch (InitializationFailedException | WinAPICallFailedException e) {
            LOGGER.error("Failed to unprotect data with WinDPAPI.", e);
            throw new KeyringException("Failed to unprotect data with WinDPAPI.", e);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to parse data from WinDPAPI.", e);
            throw new KeyringException("Failed to parse data from WinDPAPI.", e);
        }
    }

    private File getFile(){
        String windpapiPath = SettingsProvider.getInstance().getTargetDirectoryPath() + File.separator + "windpapi.json";
        return new File(windpapiPath);
    }

    @Override
    public boolean isSupported() {
        return WinDPAPI.isPlatformSupported();
    }

    @Override
    public String getIdentifier() {
        return "WinDPAPI";
    }

}
