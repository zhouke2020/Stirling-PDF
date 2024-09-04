package stirling.software.SPDF.config.security.saml;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;
import org.springframework.security.saml2.provider.service.metadata.OpenSamlMetadataResolver;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.Saml2MetadataFilter;

import lombok.extern.slf4j.Slf4j;
import stirling.software.SPDF.model.ApplicationProperties;

@Configuration
@Slf4j
public class SamlConfig {

    @Autowired ApplicationProperties applicationProperties;

    @Bean
    public OpenSaml4AuthenticationProvider openSaml4AuthenticationProvider() {
        OpenSaml4AuthenticationProvider provider = new OpenSaml4AuthenticationProvider();
        provider.setResponseAuthenticationConverter(
                responseToken -> {
                    Saml2AuthenticationToken token = responseToken.getToken();
                    log.info("Received SAML response: {}", token.getSaml2Response());
                    // Your custom conversion logic here
                    // For now, we'll just return the token as is
                    return token;
                });
        return provider;
    }

    @Bean
    @ConditionalOnProperty(
            value = "security.saml.enabled",
            havingValue = "true",
            matchIfMissing = false)
    public RelyingPartyRegistrationRepository relyingPartyRegistrationRepository() {
        RelyingPartyRegistration registration =
                RelyingPartyRegistration.withRegistrationId(
                                applicationProperties.getSecurity().getSAML().getRegistrationId())
                        .entityId(applicationProperties.getSecurity().getSAML().getEntityId())
                        .assertionConsumerServiceLocation(
                                applicationProperties.getSecurity().getSAML().getSpBaseUrl()
                                        + "/login/saml2/sso/stirling")
                        .singleLogoutServiceLocation(
                                applicationProperties.getSecurity().getSAML().getSpBaseUrl()
                                        + "/logout/saml2/slo")
                        .singleLogoutServiceResponseLocation(
                                applicationProperties.getSecurity().getSAML().getSpBaseUrl()
                                        + "/logout/saml2/slo")
                        .signingX509Credentials(credentials -> credentials.add(signingCredential()))
                        .assertingPartyDetails(
                                party ->
                                        party.entityId(
                                                        applicationProperties
                                                                .getSecurity()
                                                                .getSAML()
                                                                .getEntityId())
                                                .singleSignOnServiceLocation(
                                                        applicationProperties
                                                                .getSecurity()
                                                                .getSAML()
                                                                .getIdpMetadataLocation())
                                                .wantAuthnRequestsSigned(true)
                                                .verificationX509Credentials(
                                                        c -> c.add(this.realmCertificate())))
                        .build();
        return new InMemoryRelyingPartyRegistrationRepository(registration);
    }

    private Saml2X509Credential signingCredential() {
        log.info("Starting to load signing credential");
        try {
            Resource storeResource =
                    applicationProperties
                            .getSecurity()
                            .getSAML()
                            .getKeystore()
                            .getKeystoreResource();
            log.info("Keystore resource: {}", storeResource.getDescription());

            KeyStore keyStore = KeyStore.getInstance("JKS");
            try (InputStream is = storeResource.getInputStream()) {
                keyStore.load(
                        is,
                        applicationProperties
                                .getSecurity()
                                .getSAML()
                                .getKeystore()
                                .getKeystorePassword()
                                .toCharArray());
                log.info("Keystore loaded successfully");
            }

            String keyAlias =
                    applicationProperties.getSecurity().getSAML().getKeystore().getKeyAlias();
            log.info("Attempting to retrieve private key with alias: {}", keyAlias);

            PrivateKey privateKey =
                    (PrivateKey)
                            keyStore.getKey(
                                    keyAlias,
                                    applicationProperties
                                            .getSecurity()
                                            .getSAML()
                                            .getKeystore()
                                            .getKeyPassword()
                                            .toCharArray());

            if (privateKey == null) {
                log.error("Private key not found for alias: {}", keyAlias);
                throw new RuntimeException("Private key not found in keystore");
            }

            log.info("Private key retrieved successfully");

            X509Certificate certificate = (X509Certificate) keyStore.getCertificate(keyAlias);

            if (certificate == null) {
                log.info("Certificate not found for alias: {}", keyAlias);
                throw new RuntimeException("Certificate not found in keystore");
            }

            log.info(
                    "Certificate retrieved successfully. Subject: {}",
                    certificate.getSubjectX500Principal());

            log.info("Signing credential created successfully");
            return Saml2X509Credential.signing(privateKey, certificate);
        } catch (Exception e) {
            log.error("Error loading signing credential", e);
            throw new RuntimeException("Error loading signing credential", e);
        }
    }

    private Saml2X509Credential realmCertificate() {
        log.info("Starting to load realm certificate");
        try {
            Resource storeResource =
                    applicationProperties
                            .getSecurity()
                            .getSAML()
                            .getKeystore()
                            .getKeystoreResource();
            log.info("Keystore resource: {}", storeResource.getDescription());

            KeyStore keyStore = KeyStore.getInstance("JKS");
            try (InputStream is = storeResource.getInputStream()) {
                keyStore.load(
                        is,
                        applicationProperties
                                .getSecurity()
                                .getSAML()
                                .getKeystore()
                                .getKeystorePassword()
                                .toCharArray());
                log.info("Keystore loaded successfully");
            }

            String realmCertificateAlias =
                    applicationProperties
                            .getSecurity()
                            .getSAML()
                            .getKeystore()
                            .getRealmCertificateAlias();
            log.info(
                    "Attempting to retrieve realm certificate with alias: {}",
                    realmCertificateAlias);

            X509Certificate certificate =
                    (X509Certificate) keyStore.getCertificate(realmCertificateAlias);

            if (certificate == null) {
                log.error("Realm certificate not found for alias: {}", realmCertificateAlias);
                throw new RuntimeException("Realm certificate not found in keystore");
            }

            log.info(
                    "Realm certificate retrieved successfully. Subject: {}",
                    certificate.getSubjectX500Principal());

            log.info("Realm certificate credential created successfully");
            return Saml2X509Credential.verification(certificate);
        } catch (Exception e) {
            log.error("Error loading realm certificate", e);
            throw new RuntimeException("Error loading realm certificate", e);
        }
    }

    @Bean
    @ConditionalOnProperty(
            value = "security.saml.enabled",
            havingValue = "true",
            matchIfMissing = false)
    public Saml2MetadataFilter metadataFilter(RelyingPartyRegistrationRepository registrations) {
        DefaultRelyingPartyRegistrationResolver registrationResolver =
                new DefaultRelyingPartyRegistrationResolver(registrations);
        OpenSamlMetadataResolver metadataResolver = new OpenSamlMetadataResolver();
        return new Saml2MetadataFilter(registrationResolver, metadataResolver);
    }
}
