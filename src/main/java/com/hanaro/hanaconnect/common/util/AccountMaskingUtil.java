package com.hanaro.hanaconnect.common.util;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AccountMaskingUtil {

    private final AccountCryptoService accountCryptoService;

    // 마스킹 (기본)
    public String mask(String encryptedAccountNumber) {
        if (encryptedAccountNumber == null) {
            return null;
        }

        String plain = accountCryptoService.decrypt(encryptedAccountNumber);

        if (plain.length() != 11) {
            return plain;
        }

        return plain.substring(0, 3) + "-" +
            plain.substring(3, 7) + "-****";
    }

    // 전체 계좌번호 (필요할 때만 사용)
    public String full(String encryptedAccountNumber) {
        if (encryptedAccountNumber == null) {
            return null;
        }

        return accountCryptoService.decrypt(encryptedAccountNumber);
    }

    // 뒤 4자리만
    public String last4(String encryptedAccountNumber) {
        if (encryptedAccountNumber == null) {
            return null;
        }

        String plain = accountCryptoService.decrypt(encryptedAccountNumber);

        if (plain.length() < 4) {
            return plain;
        }

        return "****" + plain.substring(plain.length() - 4);
    }
}
