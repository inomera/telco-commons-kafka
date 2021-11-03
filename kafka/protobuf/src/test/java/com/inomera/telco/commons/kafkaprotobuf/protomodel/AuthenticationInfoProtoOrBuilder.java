package com.inomera.telco.commons.kafkaprotobuf.protomodel;
public interface AuthenticationInfoProtoOrBuilder extends com.google.protobuf.MessageOrBuilder {
    // @@protoc_insertion_point(interface_extends:com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto)
    /**
     * <code>int32 result = 1;</code>
     */
    int getResult();

    /**
     * <code>bool fromAvea = 2;</code>
     */
    boolean getFromAvea();

    /**
     * <code>string msisdn = 3;</code>
     */
    String getMsisdn();

    /**
     * <code>string msisdn = 3;</code>
     */
    com.google.protobuf.ByteString getMsisdnBytes();

    /**
     * <code>string ssoUniqueId = 4;</code>
     */
    String getSsoUniqueId();

    /**
     * <code>string ssoUniqueId = 4;</code>
     */
    com.google.protobuf.ByteString getSsoUniqueIdBytes();

    /**
     * <code>string ssoUsername = 5;</code>
     */
    String getSsoUsername();

    /**
     * <code>string ssoUsername = 5;</code>
     */
    com.google.protobuf.ByteString getSsoUsernameBytes();

    /**
     * <code>string mtsCustomerId = 6;</code>
     */
    String getMtsCustomerId();

    /**
     * <code>string mtsCustomerId = 6;</code>
     */
    com.google.protobuf.ByteString getMtsCustomerIdBytes();

    /**
     * <code>string adslNo = 7;</code>
     */
    String getAdslNo();

    /**
     * <code>string adslNo = 7;</code>
     */
    com.google.protobuf.ByteString getAdslNoBytes();

    /**
     * <code>bool fromSSO = 8;</code>
     */
    boolean getFromSSO();

    /**
     * <code>bool fromSSOMsisdn = 9;</code>
     */
    boolean getFromSSOMsisdn();

    /**
     * <code>string userType = 10;</code>
     */
    String getUserType();

    /**
     * <code>string userType = 10;</code>
     */
    com.google.protobuf.ByteString getUserTypeBytes();

    /**
     * <code>string token = 11;</code>
     */
    String getToken();

    /**
     * <code>string token = 11;</code>
     */
    com.google.protobuf.ByteString getTokenBytes();

    /**
     * <code>string firstName = 12;</code>
     */
    String getFirstName();

    /**
     * <code>string firstName = 12;</code>
     */
    com.google.protobuf.ByteString getFirstNameBytes();

    /**
     * <code>string lastName = 13;</code>
     */
    String getLastName();

    /**
     * <code>string lastName = 13;</code>
     */
    com.google.protobuf.ByteString getLastNameBytes();

    /**
     * <code>string serviceMessage = 14;</code>
     */
    String getServiceMessage();

    /**
     * <code>string serviceMessage = 14;</code>
     */
    com.google.protobuf.ByteString getServiceMessageBytes();

    /**
     * <code>bool premium = 15;</code>
     */
    boolean getPremium();

    /**
     * <code>bool fromMobileConnect = 16;</code>
     */
    boolean getFromMobileConnect();
}
