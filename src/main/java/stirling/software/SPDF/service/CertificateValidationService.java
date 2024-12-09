package stirling.software.SPDF.service;

import java.io.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.*;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CertificateValidationService {
    private KeyStore trustStore;
    private static final String AATL_RESOURCE = "/tl12.acrobatsecuritysettings";

    @PostConstruct
    private void initializeTrustStore() throws Exception {
        trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        loadAATLCertificatesFromPDF();
    }

    private void loadAATLCertificatesFromPDF() throws Exception {
        log.debug("Starting AATL certificate loading from PDF...");

        try (InputStream pdfStream = new ClassPathResource(AATL_RESOURCE).getInputStream()) {
            PDDocument document = Loader.loadPDF(pdfStream.readAllBytes());

            PDEmbeddedFilesNameTreeNode embeddedFiles =
                    document.getDocumentCatalog().getNames().getEmbeddedFiles();
            Map<String, PDComplexFileSpecification> files = embeddedFiles.getNames();

            for (Map.Entry<String, PDComplexFileSpecification> entry : files.entrySet()) {
                log.debug(entry.getKey());
                if (entry.getKey().equals("SecuritySettings.xml")) {
                    byte[] xmlContent = entry.getValue().getEmbeddedFile().toByteArray();
                    processSecuritySettingsXML(xmlContent);
                    break;
                }
            }
        }
    }

    private void processSecuritySettingsXML(byte[] xmlContent) throws Exception {
        // Simple XML parsing using String operations
        String xmlString = new String(xmlContent, "UTF-8");
        int certCount = 0;
        int failedCerts = 0;

        // Find all Certificate tags
        String startTag = "<Certificate>";
        String endTag = "</Certificate>";
        int startIndex = 0;

        while ((startIndex = xmlString.indexOf(startTag, startIndex)) != -1) {
            int endIndex = xmlString.indexOf(endTag, startIndex);
            if (endIndex == -1) break;

            // Extract certificate data
            String certData = xmlString.substring(startIndex + startTag.length(), endIndex).trim();
            startIndex = endIndex + endTag.length();

            try {
                byte[] certBytes = Base64.getDecoder().decode(certData);
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509Certificate cert =
                        (X509Certificate)
                                cf.generateCertificate(new ByteArrayInputStream(certBytes));

                // Only store root certificates (self-signed)
                if (cert.getSubjectX500Principal().equals(cert.getIssuerX500Principal())) {
                    trustStore.setCertificateEntry("aatl-cert-" + certCount, cert);
                    log.trace(
                            "Successfully loaded AATL root certificate #"
                                    + certCount
                                    + "\n  Subject: "
                                    + cert.getSubjectX500Principal().getName()
                                    + "\n  Valid until: "
                                    + cert.getNotAfter());
                    certCount++;
                }
            } catch (Exception e) {
                failedCerts++;
                log.error("Failed to process AATL certificate: " + e.getMessage());
            }
        }

        log.debug("AATL Certificate loading completed:");
        log.debug("  Total root certificates successfully loaded: " + certCount);
        log.debug("  Failed certificates: " + failedCerts);
    }

    @Data
    public static class ValidationResult {
        private boolean valid;
        private boolean expired;
        private boolean validAtSigningTime;
        private String errorMessage;
    }

    public ValidationResult validateCertificateChain(X509Certificate signerCert) {
        ValidationResult result = new ValidationResult();
        try {
            // Build the certificate chain
            List<X509Certificate> certChain = buildCertificateChain(signerCert);

            // Create certificate path
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            CertPath certPath = cf.generateCertPath(certChain);

            // Set up trust anchors
            Set<TrustAnchor> anchors = new HashSet<>();
            Enumeration<String> aliases = trustStore.aliases();
            while (aliases.hasMoreElements()) {
                Object trustCert = trustStore.getCertificate(aliases.nextElement());
                if (trustCert instanceof X509Certificate) {
                    anchors.add(new TrustAnchor((X509Certificate) trustCert, null));
                }
            }

            // Set up validation parameters
            PKIXParameters params = new PKIXParameters(anchors);
            params.setRevocationEnabled(false);

            // Validate the path
            CertPathValidator validator = CertPathValidator.getInstance("PKIX");
            validator.validate(certPath, params);

            result.setValid(true);
            result.setExpired(isExpired(signerCert));

            return result;
        } catch (Exception e) {
            result.setValid(false);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }

    public ValidationResult validateWithCustomCert(
            X509Certificate signerCert, X509Certificate customCert) {
        ValidationResult result = new ValidationResult();
        try {
            // Build the complete chain from signer cert
            List<X509Certificate> certChain = buildCertificateChain(signerCert);

            // Check if custom cert matches any cert in the chain
            boolean matchFound = false;
            for (X509Certificate chainCert : certChain) {
                if (chainCert.equals(customCert)) {
                    matchFound = true;
                    break;
                }
            }

            if (!matchFound) {
                // Check if custom cert is a valid issuer for any cert in the chain
                for (X509Certificate chainCert : certChain) {
                    try {
                        chainCert.verify(customCert.getPublicKey());
                        matchFound = true;
                        break;
                    } catch (Exception e) {
                        // Continue checking next cert
                    }
                }
            }

            result.setValid(matchFound);
            if (!matchFound) {
                result.setErrorMessage(
                        "Custom certificate is not part of the chain and is not a valid issuer");
            }

            return result;
        } catch (Exception e) {
            result.setValid(false);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }

    private List<X509Certificate> buildCertificateChain(X509Certificate signerCert)
            throws CertificateException {
        List<X509Certificate> chain = new ArrayList<>();
        chain.add(signerCert);

        X509Certificate current = signerCert;
        while (!isSelfSigned(current)) {
            X509Certificate issuer = findIssuer(current);
            if (issuer == null) break;
            chain.add(issuer);
            current = issuer;
        }

        return chain;
    }

    private boolean isSelfSigned(X509Certificate cert) {
        return cert.getSubjectX500Principal().equals(cert.getIssuerX500Principal());
    }

    private X509Certificate findIssuer(X509Certificate cert) throws CertificateException {
        try {
            Enumeration<String> aliases = trustStore.aliases();
            while (aliases.hasMoreElements()) {
                Certificate trustCert = trustStore.getCertificate(aliases.nextElement());
                if (trustCert instanceof X509Certificate) {
                    X509Certificate x509TrustCert = (X509Certificate) trustCert;
                    if (cert.getIssuerX500Principal()
                            .equals(x509TrustCert.getSubjectX500Principal())) {
                        try {
                            cert.verify(x509TrustCert.getPublicKey());
                            return x509TrustCert;
                        } catch (Exception e) {
                            // Continue searching if verification fails
                        }
                    }
                }
            }
        } catch (KeyStoreException e) {
            throw new CertificateException("Error accessing trust store", e);
        }
        return null;
    }

    private boolean isExpired(X509Certificate cert) {
        try {
            cert.checkValidity();
            return false;
        } catch (CertificateExpiredException | CertificateNotYetValidException e) {
            return true;
        }
    }
}
