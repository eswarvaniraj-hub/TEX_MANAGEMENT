package com.textile.accounts.masterdata.service;

import java.util.regex.Pattern;

public final class MasterDataValidationUtils {

    private static final Pattern GSTIN_PATTERN = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");
    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]{1}$");
    private static final Pattern HSN_PATTERN = Pattern.compile("^[0-9]{4,8}$");

    private MasterDataValidationUtils() {}

    public static String validateAndNormalizeGstin(String gstin, String stateCode) {
        if (gstin == null || gstin.isBlank()) {
            return null;
        }
        String cleanGstin = gstin.trim().toUpperCase();
        if (!GSTIN_PATTERN.matcher(cleanGstin).matches()) {
            throw new IllegalArgumentException("Invalid GSTIN format: must be 15 alphanumeric characters matching standard GSTIN pattern.");
        }
        if (stateCode != null && !stateCode.isBlank()) {
            String gstinState = cleanGstin.substring(0, 2);
            if (!gstinState.equals(stateCode.trim())) {
                throw new IllegalArgumentException(String.format(
                        "GSTIN state prefix (%s) does not match supplied state code (%s).",
                        gstinState, stateCode
                ));
            }
        }
        return cleanGstin;
    }

    public static String extractStateFromGstin(String gstin) {
        if (gstin != null && gstin.trim().length() >= 2) {
            return gstin.trim().substring(0, 2);
        }
        return null;
    }

    public static String validateAndNormalizePan(String pan) {
        if (pan == null || pan.isBlank()) {
            return null;
        }
        String cleanPan = pan.trim().toUpperCase();
        if (!PAN_PATTERN.matcher(cleanPan).matches()) {
            throw new IllegalArgumentException("Invalid PAN format: must be 10 characters matching standard Indian PAN format (e.g. ABCDE1234F).");
        }
        return cleanPan;
    }

    public static String validateAndNormalizeHsn(String hsn) {
        if (hsn == null || hsn.isBlank()) {
            return null;
        }
        String cleanHsn = hsn.trim();
        if (!HSN_PATTERN.matcher(cleanHsn).matches()) {
            throw new IllegalArgumentException("Invalid HSN format: must be 4 to 8 numeric digits.");
        }
        return cleanHsn;
    }
}
