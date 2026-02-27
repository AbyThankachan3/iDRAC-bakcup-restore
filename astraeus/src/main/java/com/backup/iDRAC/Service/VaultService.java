package com.backup.iDRAC.Service;

import com.backup.iDRAC.Entity.Credentials;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultKeyValueOperationsSupport.KeyValueBackend;
import org.springframework.vault.core.VaultTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class VaultService {

    private final VaultTemplate vaultTemplate;

    private static final String MOUNT = "secret";
    private static final String BASE_PATH = "idrac/";

    private org.springframework.vault.core.VaultKeyValueOperations kv() {
        return vaultTemplate.opsForKeyValue(MOUNT, KeyValueBackend.KV_2);
    }

    public String storeCredentials(String host, String username, String password) {
        String path = BASE_PATH + host;
        kv().put(path, Map.of("username", username, "password", password));
        return path;
    }

    public Credentials getCredentials(String vaultPath) {
        var response = kv().get(vaultPath);
        if (response == null || response.getData() == null) {
            throw new RuntimeException("Credentials not found for: " + vaultPath);
        }
        var data = response.getData();
        return new Credentials(
                (String) data.get("username"),
                (String) data.get("password")
        );
    }

    public void deleteCredentials(String vaultPath) {
        vaultTemplate.delete(vaultPath);
    }
}