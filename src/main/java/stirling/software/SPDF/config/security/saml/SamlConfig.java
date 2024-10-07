package stirling.software.SPDF.config.security.saml;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;

import org.opensaml.security.x509.X509Support;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;

import lombok.extern.slf4j.Slf4j;
import stirling.software.SPDF.model.ApplicationProperties;

@Configuration
@Slf4j
public class SamlConfig {

    @Autowired ApplicationProperties applicationProperties;

    @Autowired ResourceLoader resourceLoader;

    @Bean
    @ConditionalOnProperty(
            value = "security.saml.enabled",
            havingValue = "true",
            matchIfMissing = false)
    public RelyingPartyRegistrationRepository relyingPartyRegistrationRepository()
            throws CertificateException, IOException {

        //        Resource signingCertResource = new ClassPathResource(this.rpSigningCertLocation);
        Resource signingCertResource =
                resourceLoader.getResource(
                        this.applicationProperties
                                .getSecurity()
                                .getSaml()
                                .getCertificateLocation());
        //        Resource signingKeyResource = new ClassPathResource(this.rpSigningKeyLocation);
        Resource signingKeyResource =
                resourceLoader.getResource(
                        this.applicationProperties.getSecurity().getSaml().getPrivateKeyLocation());
        try (InputStream is = signingKeyResource.getInputStream();
                InputStream certIS = signingCertResource.getInputStream(); ) {
            X509Certificate rpCertificate = X509Support.decodeCertificate(certIS.readAllBytes());
            RSAPrivateKey rpKey = RsaKeyConverters.pkcs8().convert(is);
            final Saml2X509Credential rpSigningCredentials =
                    Saml2X509Credential.signing(rpKey, rpCertificate);

            X509Certificate apCert =
                    X509Support.decodeCertificate(
                            applicationProperties.getSecurity().getSaml().getSigningCertificate());
            Saml2X509Credential apCredential = Saml2X509Credential.verification(apCert);

            RelyingPartyRegistration registration =
                    RelyingPartyRegistrations.fromMetadataLocation(
                                    applicationProperties
                                            .getSecurity()
                                            .getSaml()
                                            .getIdpMetadataLocation())
                            .entityId(applicationProperties.getSecurity().getSaml().getEntityId())
                            .registrationId(
                                    applicationProperties
                                            .getSecurity()
                                            .getSaml()
                                            .getRegistrationId())
                            .signingX509Credentials(c -> c.add(rpSigningCredentials))
                            .assertingPartyDetails(
                                    party ->
                                            party.wantAuthnRequestsSigned(true)
                                                    .verificationX509Credentials(
                                                            c -> c.add(apCredential)))
                            .build();
            return new InMemoryRelyingPartyRegistrationRepository(registration);
        }
    }
}
