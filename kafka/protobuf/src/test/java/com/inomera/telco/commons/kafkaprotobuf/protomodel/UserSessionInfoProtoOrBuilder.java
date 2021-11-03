package com.inomera.telco.commons.kafkaprotobuf.protomodel;

public interface UserSessionInfoProtoOrBuilder extends com.google.protobuf.MessageOrBuilder {
    // @@protoc_insertion_point(interface_extends:com.inomera.telco.commons.kafkaprotobuf.protomodel.UserSessionInfoProto)

    /**
     * <code>string userId = 1;</code>
     */
    String getUserId();

    /**
     * <code>string userId = 1;</code>
     */
    com.google.protobuf.ByteString getUserIdBytes();

    /**
     * <code>string sessionKey = 2;</code>
     */
    String getSessionKey();

    /**
     * <code>string sessionKey = 2;</code>
     */
    com.google.protobuf.ByteString getSessionKeyBytes();

    /**
     * <code>int64 lastActivationTime = 3;</code>
     */
    long getLastActivationTime();

    /**
     * <code>.com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto authenticationInfo = 4;</code>
     */
    boolean hasAuthenticationInfo();

    /**
     * <code>.com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto authenticationInfo = 4;</code>
     */
    AuthenticationInfoProto getAuthenticationInfo();

    /**
     * <code>.com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto authenticationInfo = 4;</code>
     */
    AuthenticationInfoProtoOrBuilder getAuthenticationInfoOrBuilder();

    /**
     * <code>int32 requestCount = 5;</code>
     */
    int getRequestCount();

    /**
     * <code>string deviceId = 6;</code>
     */
    String getDeviceId();

    /**
     * <code>string deviceId = 6;</code>
     */
    com.google.protobuf.ByteString getDeviceIdBytes();

    /**
     * <code>string deviceName = 7;</code>
     */
    String getDeviceName();

    /**
     * <code>string deviceName = 7;</code>
     */
    com.google.protobuf.ByteString getDeviceNameBytes();
}
