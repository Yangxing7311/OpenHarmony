package org.example.model;

import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * 设备证书信息
 */
public class DeviceCertificate {
    private String subject;
    private String issuer;
    private String serialNumber;
    private Date notBefore;
    private Date notAfter;
    private X509Certificate x509Certificate;

    // Getters and Setters
    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public Date getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(Date notBefore) {
        this.notBefore = notBefore;
    }

    public Date getNotAfter() {
        return notAfter;
    }

    public void setNotAfter(Date notAfter) {
        this.notAfter = notAfter;
    }

    public X509Certificate getX509Certificate() {
        return x509Certificate;
    }

    public void setX509Certificate(X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate;
    }

    @Override
    public String toString() {
        return "DeviceCertificate{" +
                "subject='" + subject + '\'' +
                ", issuer='" + issuer + '\'' +
                ", serialNumber='" + serialNumber + '\'' +
                ", notBefore=" + notBefore +
                ", notAfter=" + notAfter +
                '}';
    }
}

