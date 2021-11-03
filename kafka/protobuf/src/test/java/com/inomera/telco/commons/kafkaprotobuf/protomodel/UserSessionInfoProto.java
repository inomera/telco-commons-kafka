package com.inomera.telco.commons.kafkaprotobuf.protomodel;
/**
 * Protobuf type {@code com.inomera.telco.commons.kafkaprotobuf.protomodel.UserSessionInfoProto}
 */
// @@protoc_insertion_point(message_implements:com.inomera.telco.commons.kafkaprotobuf.protomodel.UserSessionInfoProto)
public final class UserSessionInfoProto extends com.google.protobuf.GeneratedMessageV3 implements com.inomera.telco.commons.kafkaprotobuf.protomodel.UserSessionInfoProtoOrBuilder {
    private static final long serialVersionUID = 0L;

    // Use UserSessionInfoProto.newBuilder() to construct.
    private UserSessionInfoProto(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
        super(builder);
    }

    private UserSessionInfoProto() {
        userId_ = "";
        sessionKey_ = "";
        deviceId_ = "";
        deviceName_ = "";
    }

    @Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
        return this.unknownFields;
    }

    private UserSessionInfoProto(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        this();
        if (extensionRegistry == null) {
            throw new NullPointerException();
        }
        int mutable_bitField0_ = 0;
        com.google.protobuf.UnknownFieldSet.Builder unknownFields = com.google.protobuf.UnknownFieldSet.newBuilder();
        try {
            boolean done = false;
            while (!done) {
                int tag = input.readTag();
                switch (tag) {
                    case 0 :
                        done = true;
                        break;
                    case 10 :
                        {
                            String s = input.readStringRequireUtf8();
                            userId_ = s;
                            break;
                        }
                    case 18 :
                        {
                            String s = input.readStringRequireUtf8();
                            sessionKey_ = s;
                            break;
                        }
                    case 24 :
                        {
                            lastActivationTime_ = input.readInt64();
                            break;
                        }
                    case 34 :
                        {
                            com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto.Builder subBuilder = null;
                            if (authenticationInfo_ != null) {
                                subBuilder = authenticationInfo_.toBuilder();
                            }
                            authenticationInfo_ = input.readMessage(com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto.parser(), extensionRegistry);
                            if (subBuilder != null) {
                                subBuilder.mergeFrom(authenticationInfo_);
                                authenticationInfo_ = subBuilder.buildPartial();
                            }
                            break;
                        }
                    case 40 :
                        {
                            requestCount_ = input.readInt32();
                            break;
                        }
                    case 50 :
                        {
                            String s = input.readStringRequireUtf8();
                            deviceId_ = s;
                            break;
                        }
                    case 58 :
                        {
                            String s = input.readStringRequireUtf8();
                            deviceName_ = s;
                            break;
                        }
                    default :
                        {
                            if (!parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
                                done = true;
                            }
                            break;
                        }
                }
            } 
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
            throw e.setUnfinishedMessage(this);
        } catch (java.io.IOException e) {
            throw new com.google.protobuf.InvalidProtocolBufferException(e).setUnfinishedMessage(this);
        } finally {
            this.unknownFields = unknownFields.build();
            makeExtensionsImmutable();
        }
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return com.inomera.telco.commons.kafkaprotobuf.protomodel.UserSessionInfoProtoOuterClass.internal_static_com_inomera_muud_domain_session_UserSessionInfoProto_descriptor;
    }

    @Override
    protected FieldAccessorTable internalGetFieldAccessorTable() {
        return com.inomera.telco.commons.kafkaprotobuf.protomodel.UserSessionInfoProtoOuterClass.internal_static_com_inomera_muud_domain_session_UserSessionInfoProto_fieldAccessorTable.ensureFieldAccessorsInitialized(UserSessionInfoProto.class, Builder.class);
    }

    public static final int USERID_FIELD_NUMBER = 1;

    private volatile Object userId_;

    /**
     * <code>string userId = 1;</code>
     */
    public String getUserId() {
        Object ref = userId_;
        if (ref instanceof String) {
            return ((String) (ref));
        } else {
            com.google.protobuf.ByteString bs = ((com.google.protobuf.ByteString) (ref));
            String s = bs.toStringUtf8();
            userId_ = s;
            return s;
        }
    }

    /**
     * <code>string userId = 1;</code>
     */
    public com.google.protobuf.ByteString getUserIdBytes() {
        Object ref = userId_;
        if (ref instanceof String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8(((String) (ref)));
            userId_ = b;
            return b;
        } else {
            return ((com.google.protobuf.ByteString) (ref));
        }
    }

    public static final int SESSIONKEY_FIELD_NUMBER = 2;

    private volatile Object sessionKey_;

    /**
     * <code>string sessionKey = 2;</code>
     */
    public String getSessionKey() {
        Object ref = sessionKey_;
        if (ref instanceof String) {
            return ((String) (ref));
        } else {
            com.google.protobuf.ByteString bs = ((com.google.protobuf.ByteString) (ref));
            String s = bs.toStringUtf8();
            sessionKey_ = s;
            return s;
        }
    }

    /**
     * <code>string sessionKey = 2;</code>
     */
    public com.google.protobuf.ByteString getSessionKeyBytes() {
        Object ref = sessionKey_;
        if (ref instanceof String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8(((String) (ref)));
            sessionKey_ = b;
            return b;
        } else {
            return ((com.google.protobuf.ByteString) (ref));
        }
    }

    public static final int LASTACTIVATIONTIME_FIELD_NUMBER = 3;

    private long lastActivationTime_;

    /**
     * <code>int64 lastActivationTime = 3;</code>
     */
    public long getLastActivationTime() {
        return lastActivationTime_;
    }

    public static final int AUTHENTICATIONINFO_FIELD_NUMBER = 4;

    private com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto authenticationInfo_;

    /**
     * <code>.com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto authenticationInfo = 4;</code>
     */
    public boolean hasAuthenticationInfo() {
        return authenticationInfo_ != null;
    }

    /**
     * <code>.com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto authenticationInfo = 4;</code>
     */
    public com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto getAuthenticationInfo() {
        return authenticationInfo_ == null ? com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto.getDefaultInstance() : authenticationInfo_;
    }

    /**
     * <code>.com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto authenticationInfo = 4;</code>
     */
    public com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProtoOrBuilder getAuthenticationInfoOrBuilder() {
        return getAuthenticationInfo();
    }

    public static final int REQUESTCOUNT_FIELD_NUMBER = 5;

    private int requestCount_;

    /**
     * <code>int32 requestCount = 5;</code>
     */
    public int getRequestCount() {
        return requestCount_;
    }

    public static final int DEVICEID_FIELD_NUMBER = 6;

    private volatile Object deviceId_;

    /**
     * <code>string deviceId = 6;</code>
     */
    public String getDeviceId() {
        Object ref = deviceId_;
        if (ref instanceof String) {
            return ((String) (ref));
        } else {
            com.google.protobuf.ByteString bs = ((com.google.protobuf.ByteString) (ref));
            String s = bs.toStringUtf8();
            deviceId_ = s;
            return s;
        }
    }

    /**
     * <code>string deviceId = 6;</code>
     */
    public com.google.protobuf.ByteString getDeviceIdBytes() {
        Object ref = deviceId_;
        if (ref instanceof String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8(((String) (ref)));
            deviceId_ = b;
            return b;
        } else {
            return ((com.google.protobuf.ByteString) (ref));
        }
    }

    public static final int DEVICENAME_FIELD_NUMBER = 7;

    private volatile Object deviceName_;

    /**
     * <code>string deviceName = 7;</code>
     */
    public String getDeviceName() {
        Object ref = deviceName_;
        if (ref instanceof String) {
            return ((String) (ref));
        } else {
            com.google.protobuf.ByteString bs = ((com.google.protobuf.ByteString) (ref));
            String s = bs.toStringUtf8();
            deviceName_ = s;
            return s;
        }
    }

    /**
     * <code>string deviceName = 7;</code>
     */
    public com.google.protobuf.ByteString getDeviceNameBytes() {
        Object ref = deviceName_;
        if (ref instanceof String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8(((String) (ref)));
            deviceName_ = b;
            return b;
        } else {
            return ((com.google.protobuf.ByteString) (ref));
        }
    }

    private byte memoizedIsInitialized = -1;

    @Override
    public final boolean isInitialized() {
        byte isInitialized = memoizedIsInitialized;
        if (isInitialized == 1)
            return true;

        if (isInitialized == 0)
            return false;

        memoizedIsInitialized = 1;
        return true;
    }

    @Override
    public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
        if (!getUserIdBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 1, userId_);
        }
        if (!getSessionKeyBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 2, sessionKey_);
        }
        if (lastActivationTime_ != 0L) {
            output.writeInt64(3, lastActivationTime_);
        }
        if (authenticationInfo_ != null) {
            output.writeMessage(4, getAuthenticationInfo());
        }
        if (requestCount_ != 0) {
            output.writeInt32(5, requestCount_);
        }
        if (!getDeviceIdBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 6, deviceId_);
        }
        if (!getDeviceNameBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 7, deviceName_);
        }
        unknownFields.writeTo(output);
    }

    @Override
    public int getSerializedSize() {
        int size = memoizedSize;
        if (size != (-1))
            return size;

        size = 0;
        if (!getUserIdBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, userId_);
        }
        if (!getSessionKeyBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, sessionKey_);
        }
        if (lastActivationTime_ != 0L) {
            size += com.google.protobuf.CodedOutputStream.computeInt64Size(3, lastActivationTime_);
        }
        if (authenticationInfo_ != null) {
            size += com.google.protobuf.CodedOutputStream.computeMessageSize(4, getAuthenticationInfo());
        }
        if (requestCount_ != 0) {
            size += com.google.protobuf.CodedOutputStream.computeInt32Size(5, requestCount_);
        }
        if (!getDeviceIdBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(6, deviceId_);
        }
        if (!getDeviceNameBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(7, deviceName_);
        }
        size += unknownFields.getSerializedSize();
        memoizedSize = size;
        return size;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof UserSessionInfoProto)) {
            return super.equals(obj);
        }
        UserSessionInfoProto other = ((UserSessionInfoProto) (obj));
        if (!getUserId().equals(other.getUserId()))
            return false;

        if (!getSessionKey().equals(other.getSessionKey()))
            return false;

        if (getLastActivationTime() != other.getLastActivationTime())
            return false;

        if (hasAuthenticationInfo() != other.hasAuthenticationInfo())
            return false;

        if (hasAuthenticationInfo()) {
            if (!getAuthenticationInfo().equals(other.getAuthenticationInfo()))
                return false;

        }
        if (getRequestCount() != other.getRequestCount())
            return false;

        if (!getDeviceId().equals(other.getDeviceId()))
            return false;

        if (!getDeviceName().equals(other.getDeviceName()))
            return false;

        if (!unknownFields.equals(other.unknownFields))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        if (memoizedHashCode != 0) {
            return memoizedHashCode;
        }
        int hash = 41;
        hash = (19 * hash) + UserSessionInfoProto.getDescriptor().hashCode();
        hash = (37 * hash) + UserSessionInfoProto.USERID_FIELD_NUMBER;
        hash = (53 * hash) + getUserId().hashCode();
        hash = (37 * hash) + UserSessionInfoProto.SESSIONKEY_FIELD_NUMBER;
        hash = (53 * hash) + getSessionKey().hashCode();
        hash = (37 * hash) + UserSessionInfoProto.LASTACTIVATIONTIME_FIELD_NUMBER;
        hash = (53 * hash) + com.google.protobuf.Internal.hashLong(getLastActivationTime());
        if (hasAuthenticationInfo()) {
            hash = (37 * hash) + UserSessionInfoProto.AUTHENTICATIONINFO_FIELD_NUMBER;
            hash = (53 * hash) + getAuthenticationInfo().hashCode();
        }
        hash = (37 * hash) + UserSessionInfoProto.REQUESTCOUNT_FIELD_NUMBER;
        hash = (53 * hash) + getRequestCount();
        hash = (37 * hash) + UserSessionInfoProto.DEVICEID_FIELD_NUMBER;
        hash = (53 * hash) + getDeviceId().hashCode();
        hash = (37 * hash) + UserSessionInfoProto.DEVICENAME_FIELD_NUMBER;
        hash = (53 * hash) + getDeviceName().hashCode();
        hash = (29 * hash) + unknownFields.hashCode();
        memoizedHashCode = hash;
        return hash;
    }

    public static UserSessionInfoProto parseFrom(java.nio.ByteBuffer data) throws com.google.protobuf.InvalidProtocolBufferException {
        return UserSessionInfoProto.PARSER.parseFrom(data);
    }

    public static UserSessionInfoProto parseFrom(java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return UserSessionInfoProto.PARSER.parseFrom(data, extensionRegistry);
    }

    public static UserSessionInfoProto parseFrom(com.google.protobuf.ByteString data) throws com.google.protobuf.InvalidProtocolBufferException {
        return UserSessionInfoProto.PARSER.parseFrom(data);
    }

    public static UserSessionInfoProto parseFrom(com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return UserSessionInfoProto.PARSER.parseFrom(data, extensionRegistry);
    }

    public static UserSessionInfoProto parseFrom(byte[] data) throws com.google.protobuf.InvalidProtocolBufferException {
        return UserSessionInfoProto.PARSER.parseFrom(data);
    }

    public static UserSessionInfoProto parseFrom(byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return UserSessionInfoProto.PARSER.parseFrom(data, extensionRegistry);
    }

    public static UserSessionInfoProto parseFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(UserSessionInfoProto.PARSER, input);
    }

    public static UserSessionInfoProto parseFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(UserSessionInfoProto.PARSER, input, extensionRegistry);
    }

    public static UserSessionInfoProto parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(UserSessionInfoProto.PARSER, input);
    }

    public static UserSessionInfoProto parseDelimitedFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(UserSessionInfoProto.PARSER, input, extensionRegistry);
    }

    public static UserSessionInfoProto parseFrom(com.google.protobuf.CodedInputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(UserSessionInfoProto.PARSER, input);
    }

    public static UserSessionInfoProto parseFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(UserSessionInfoProto.PARSER, input, extensionRegistry);
    }

    @Override
    public Builder newBuilderForType() {
        return UserSessionInfoProto.newBuilder();
    }

    public static Builder newBuilder() {
        return UserSessionInfoProto.DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(UserSessionInfoProto prototype) {
        return UserSessionInfoProto.DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    @Override
    public Builder toBuilder() {
        return this == UserSessionInfoProto.DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(BuilderParent parent) {
        Builder builder = new Builder(parent);
        return builder;
    }

    /**
     * Protobuf type {@code com.inomera.telco.commons.kafkaprotobuf.protomodel.UserSessionInfoProto}
     */
    // @@protoc_insertion_point(builder_implements:com.inomera.telco.commons.kafkaprotobuf.protomodel.UserSessionInfoProto)
    // @@protoc_insertion_point(builder_scope:com.inomera.telco.commons.kafkaprotobuf.protomodel.UserSessionInfoProto)
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements com.inomera.telco.commons.kafkaprotobuf.protomodel.UserSessionInfoProtoOrBuilder {
        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return com.inomera.telco.commons.kafkaprotobuf.protomodel.UserSessionInfoProtoOuterClass.internal_static_com_inomera_muud_domain_session_UserSessionInfoProto_descriptor;
        }

        @Override
        protected FieldAccessorTable internalGetFieldAccessorTable() {
            return com.inomera.telco.commons.kafkaprotobuf.protomodel.UserSessionInfoProtoOuterClass.internal_static_com_inomera_muud_domain_session_UserSessionInfoProto_fieldAccessorTable.ensureFieldAccessorsInitialized(UserSessionInfoProto.class, Builder.class);
        }

        // Construct using com.inomera.telco.commons.kafkaprotobuf.protomodel.UserSessionInfoProto.newBuilder()
        private Builder() {
            maybeForceBuilderInitialization();
        }

        private Builder(BuilderParent parent) {
            super(parent);
            maybeForceBuilderInitialization();
        }

        private void maybeForceBuilderInitialization() {
            if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
            }
        }

        @Override
        public Builder clear() {
            super.clear();
            userId_ = "";
            sessionKey_ = "";
            lastActivationTime_ = 0L;
            if (authenticationInfoBuilder_ == null) {
                authenticationInfo_ = null;
            } else {
                authenticationInfo_ = null;
                authenticationInfoBuilder_ = null;
            }
            requestCount_ = 0;
            deviceId_ = "";
            deviceName_ = "";
            return this;
        }

        @Override
        public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
            return com.inomera.telco.commons.kafkaprotobuf.protomodel.UserSessionInfoProtoOuterClass.internal_static_com_inomera_muud_domain_session_UserSessionInfoProto_descriptor;
        }

        @Override
        public UserSessionInfoProto getDefaultInstanceForType() {
            return UserSessionInfoProto.getDefaultInstance();
        }

        @Override
        public UserSessionInfoProto build() {
            UserSessionInfoProto result = buildPartial();
            if (!result.isInitialized()) {
                throw com.google.protobuf.AbstractMessage.Builder.newUninitializedMessageException(result);
            }
            return result;
        }

        @Override
        public UserSessionInfoProto buildPartial() {
            UserSessionInfoProto result = new UserSessionInfoProto(this);
            result.userId_ = userId_;
            result.sessionKey_ = sessionKey_;
            result.lastActivationTime_ = lastActivationTime_;
            if (authenticationInfoBuilder_ == null) {
                result.authenticationInfo_ = authenticationInfo_;
            } else {
                result.authenticationInfo_ = authenticationInfoBuilder_.build();
            }
            result.requestCount_ = requestCount_;
            result.deviceId_ = deviceId_;
            result.deviceName_ = deviceName_;
            onBuilt();
            return result;
        }

        @Override
        public Builder clone() {
            return super.clone();
        }

        @Override
        public Builder setField(com.google.protobuf.Descriptors.FieldDescriptor field, Object value) {
            return super.setField(field, value);
        }

        @Override
        public Builder clearField(com.google.protobuf.Descriptors.FieldDescriptor field) {
            return super.clearField(field);
        }

        @Override
        public Builder clearOneof(com.google.protobuf.Descriptors.OneofDescriptor oneof) {
            return super.clearOneof(oneof);
        }

        @Override
        public Builder setRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field, int index, Object value) {
            return super.setRepeatedField(field, index, value);
        }

        @Override
        public Builder addRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field, Object value) {
            return super.addRepeatedField(field, value);
        }

        @Override
        public Builder mergeFrom(com.google.protobuf.Message other) {
            if (other instanceof UserSessionInfoProto) {
                return mergeFrom(((UserSessionInfoProto) (other)));
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(UserSessionInfoProto other) {
            if (other == UserSessionInfoProto.getDefaultInstance())
                return this;

            if (!other.getUserId().isEmpty()) {
                userId_ = other.userId_;
                onChanged();
            }
            if (!other.getSessionKey().isEmpty()) {
                sessionKey_ = other.sessionKey_;
                onChanged();
            }
            if (other.getLastActivationTime() != 0L) {
                setLastActivationTime(other.getLastActivationTime());
            }
            if (other.hasAuthenticationInfo()) {
                mergeAuthenticationInfo(other.getAuthenticationInfo());
            }
            if (other.getRequestCount() != 0) {
                setRequestCount(other.getRequestCount());
            }
            if (!other.getDeviceId().isEmpty()) {
                deviceId_ = other.deviceId_;
                onChanged();
            }
            if (!other.getDeviceName().isEmpty()) {
                deviceName_ = other.deviceName_;
                onChanged();
            }
            this.mergeUnknownFields(other.unknownFields);
            onChanged();
            return this;
        }

        @Override
        public final boolean isInitialized() {
            return true;
        }

        @Override
        public Builder mergeFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
            UserSessionInfoProto parsedMessage = null;
            try {
                parsedMessage = UserSessionInfoProto.PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                parsedMessage = ((UserSessionInfoProto) (e.getUnfinishedMessage()));
                throw e.unwrapIOException();
            } finally {
                if (parsedMessage != null) {
                    mergeFrom(parsedMessage);
                }
            }
            return this;
        }

        private Object userId_ = "";

        /**
         * <code>string userId = 1;</code>
         */
        public String getUserId() {
            Object ref = userId_;
            if (!(ref instanceof String)) {
                com.google.protobuf.ByteString bs = ((com.google.protobuf.ByteString) (ref));
                String s = bs.toStringUtf8();
                userId_ = s;
                return s;
            } else {
                return ((String) (ref));
            }
        }

        /**
         * <code>string userId = 1;</code>
         */
        public com.google.protobuf.ByteString getUserIdBytes() {
            Object ref = userId_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8(((String) (ref)));
                userId_ = b;
                return b;
            } else {
                return ((com.google.protobuf.ByteString) (ref));
            }
        }

        /**
         * <code>string userId = 1;</code>
         */
        public Builder setUserId(String value) {
            if (value == null ){return this;};
            if (value == null) {
                throw new NullPointerException();
            }
            userId_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>string userId = 1;</code>
         */
        public Builder clearUserId() {
            userId_ = UserSessionInfoProto.getDefaultInstance().getUserId();
            onChanged();
            return this;
        }

        /**
         * <code>string userId = 1;</code>
         */
        public Builder setUserIdBytes(com.google.protobuf.ByteString value) {
            if (value == null ){return this;};
            if (value == null) {
                throw new NullPointerException();
            }
            com.google.protobuf.AbstractMessageLite.checkByteStringIsUtf8(value);
            userId_ = value;
            onChanged();
            return this;
        }

        private Object sessionKey_ = "";

        /**
         * <code>string sessionKey = 2;</code>
         */
        public String getSessionKey() {
            Object ref = sessionKey_;
            if (!(ref instanceof String)) {
                com.google.protobuf.ByteString bs = ((com.google.protobuf.ByteString) (ref));
                String s = bs.toStringUtf8();
                sessionKey_ = s;
                return s;
            } else {
                return ((String) (ref));
            }
        }

        /**
         * <code>string sessionKey = 2;</code>
         */
        public com.google.protobuf.ByteString getSessionKeyBytes() {
            Object ref = sessionKey_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8(((String) (ref)));
                sessionKey_ = b;
                return b;
            } else {
                return ((com.google.protobuf.ByteString) (ref));
            }
        }

        /**
         * <code>string sessionKey = 2;</code>
         */
        public Builder setSessionKey(String value) {
            if (value == null ){return this;};
            if (value == null) {
                throw new NullPointerException();
            }
            sessionKey_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>string sessionKey = 2;</code>
         */
        public Builder clearSessionKey() {
            sessionKey_ = UserSessionInfoProto.getDefaultInstance().getSessionKey();
            onChanged();
            return this;
        }

        /**
         * <code>string sessionKey = 2;</code>
         */
        public Builder setSessionKeyBytes(com.google.protobuf.ByteString value) {
            if (value == null ){return this;};
            if (value == null) {
                throw new NullPointerException();
            }
            com.google.protobuf.AbstractMessageLite.checkByteStringIsUtf8(value);
            sessionKey_ = value;
            onChanged();
            return this;
        }

        private long lastActivationTime_;

        /**
         * <code>int64 lastActivationTime = 3;</code>
         */
        public long getLastActivationTime() {
            return lastActivationTime_;
        }

        /**
         * <code>int64 lastActivationTime = 3;</code>
         */
        public Builder setLastActivationTime(long value) {
            lastActivationTime_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>int64 lastActivationTime = 3;</code>
         */
        public Builder clearLastActivationTime() {
            lastActivationTime_ = 0L;
            onChanged();
            return this;
        }

        private com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto authenticationInfo_;

        private com.google.protobuf.SingleFieldBuilderV3<com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto, com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto.Builder, com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProtoOrBuilder> authenticationInfoBuilder_;

        /**
         * <code>.com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto authenticationInfo = 4;</code>
         */
        public boolean hasAuthenticationInfo() {
            return (authenticationInfoBuilder_ != null) || (authenticationInfo_ != null);
        }

        /**
         * <code>.com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto authenticationInfo = 4;</code>
         */
        public com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto getAuthenticationInfo() {
            if (authenticationInfoBuilder_ == null) {
                return authenticationInfo_ == null ? com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto.getDefaultInstance() : authenticationInfo_;
            } else {
                return authenticationInfoBuilder_.getMessage();
            }
        }

        /**
         * <code>.com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto authenticationInfo = 4;</code>
         */
        public Builder setAuthenticationInfo(com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto value) {
            if (value == null ){return this;};
            if (authenticationInfoBuilder_ == null) {
                if (value == null) {
                    throw new NullPointerException();
                }
                authenticationInfo_ = value;
                onChanged();
            } else {
                authenticationInfoBuilder_.setMessage(value);
            }
            return this;
        }

        /**
         * <code>.com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto authenticationInfo = 4;</code>
         */
        public Builder setAuthenticationInfo(com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto.Builder builderForValue) {
            if (builderForValue == null ){return this;};
            if (authenticationInfoBuilder_ == null) {
                authenticationInfo_ = builderForValue.build();
                onChanged();
            } else {
                authenticationInfoBuilder_.setMessage(builderForValue.build());
            }
            return this;
        }

        /**
         * <code>.com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto authenticationInfo = 4;</code>
         */
        public Builder mergeAuthenticationInfo(com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto value) {
            if (authenticationInfoBuilder_ == null) {
                if (authenticationInfo_ != null) {
                    authenticationInfo_ = com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto.newBuilder(authenticationInfo_).mergeFrom(value).buildPartial();
                } else {
                    authenticationInfo_ = value;
                }
                onChanged();
            } else {
                authenticationInfoBuilder_.mergeFrom(value);
            }
            return this;
        }

        /**
         * <code>.com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto authenticationInfo = 4;</code>
         */
        public Builder clearAuthenticationInfo() {
            if (authenticationInfoBuilder_ == null) {
                authenticationInfo_ = null;
                onChanged();
            } else {
                authenticationInfo_ = null;
                authenticationInfoBuilder_ = null;
            }
            return this;
        }

        /**
         * <code>.com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto authenticationInfo = 4;</code>
         */
        public com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto.Builder getAuthenticationInfoBuilder() {
            onChanged();
            return getAuthenticationInfoFieldBuilder().getBuilder();
        }

        /**
         * <code>.com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto authenticationInfo = 4;</code>
         */
        public com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProtoOrBuilder getAuthenticationInfoOrBuilder() {
            if (authenticationInfoBuilder_ != null) {
                return authenticationInfoBuilder_.getMessageOrBuilder();
            } else {
                return authenticationInfo_ == null ? com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto.getDefaultInstance() : authenticationInfo_;
            }
        }

        /**
         * <code>.com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto authenticationInfo = 4;</code>
         */
        private com.google.protobuf.SingleFieldBuilderV3<com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto, com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto.Builder, com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProtoOrBuilder> getAuthenticationInfoFieldBuilder() {
            if (authenticationInfoBuilder_ == null) {
                authenticationInfoBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto, com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto.Builder, com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProtoOrBuilder>(getAuthenticationInfo(), getParentForChildren(), isClean());
                authenticationInfo_ = null;
            }
            return authenticationInfoBuilder_;
        }

        private int requestCount_;

        /**
         * <code>int32 requestCount = 5;</code>
         */
        public int getRequestCount() {
            return requestCount_;
        }

        /**
         * <code>int32 requestCount = 5;</code>
         */
        public Builder setRequestCount(int value) {
            requestCount_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>int32 requestCount = 5;</code>
         */
        public Builder clearRequestCount() {
            requestCount_ = 0;
            onChanged();
            return this;
        }

        private Object deviceId_ = "";

        /**
         * <code>string deviceId = 6;</code>
         */
        public String getDeviceId() {
            Object ref = deviceId_;
            if (!(ref instanceof String)) {
                com.google.protobuf.ByteString bs = ((com.google.protobuf.ByteString) (ref));
                String s = bs.toStringUtf8();
                deviceId_ = s;
                return s;
            } else {
                return ((String) (ref));
            }
        }

        /**
         * <code>string deviceId = 6;</code>
         */
        public com.google.protobuf.ByteString getDeviceIdBytes() {
            Object ref = deviceId_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8(((String) (ref)));
                deviceId_ = b;
                return b;
            } else {
                return ((com.google.protobuf.ByteString) (ref));
            }
        }

        /**
         * <code>string deviceId = 6;</code>
         */
        public Builder setDeviceId(String value) {
            if (value == null ){return this;};
            if (value == null) {
                throw new NullPointerException();
            }
            deviceId_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>string deviceId = 6;</code>
         */
        public Builder clearDeviceId() {
            deviceId_ = UserSessionInfoProto.getDefaultInstance().getDeviceId();
            onChanged();
            return this;
        }

        /**
         * <code>string deviceId = 6;</code>
         */
        public Builder setDeviceIdBytes(com.google.protobuf.ByteString value) {
            if (value == null ){return this;};
            if (value == null) {
                throw new NullPointerException();
            }
            com.google.protobuf.AbstractMessageLite.checkByteStringIsUtf8(value);
            deviceId_ = value;
            onChanged();
            return this;
        }

        private Object deviceName_ = "";

        /**
         * <code>string deviceName = 7;</code>
         */
        public String getDeviceName() {
            Object ref = deviceName_;
            if (!(ref instanceof String)) {
                com.google.protobuf.ByteString bs = ((com.google.protobuf.ByteString) (ref));
                String s = bs.toStringUtf8();
                deviceName_ = s;
                return s;
            } else {
                return ((String) (ref));
            }
        }

        /**
         * <code>string deviceName = 7;</code>
         */
        public com.google.protobuf.ByteString getDeviceNameBytes() {
            Object ref = deviceName_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8(((String) (ref)));
                deviceName_ = b;
                return b;
            } else {
                return ((com.google.protobuf.ByteString) (ref));
            }
        }

        /**
         * <code>string deviceName = 7;</code>
         */
        public Builder setDeviceName(String value) {
            if (value == null ){return this;};
            if (value == null) {
                throw new NullPointerException();
            }
            deviceName_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>string deviceName = 7;</code>
         */
        public Builder clearDeviceName() {
            deviceName_ = UserSessionInfoProto.getDefaultInstance().getDeviceName();
            onChanged();
            return this;
        }

        /**
         * <code>string deviceName = 7;</code>
         */
        public Builder setDeviceNameBytes(com.google.protobuf.ByteString value) {
            if (value == null ){return this;};
            if (value == null) {
                throw new NullPointerException();
            }
            com.google.protobuf.AbstractMessageLite.checkByteStringIsUtf8(value);
            deviceName_ = value;
            onChanged();
            return this;
        }

        @Override
        public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
            if (unknownFields == null ){return this;};
            return super.setUnknownFields(unknownFields);
        }

        @Override
        public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
            return super.mergeUnknownFields(unknownFields);
        }
    }

    // @@protoc_insertion_point(class_scope:com.inomera.telco.commons.kafkaprotobuf.protomodel.UserSessionInfoProto)
    private static final UserSessionInfoProto DEFAULT_INSTANCE;

    static {
        DEFAULT_INSTANCE = new UserSessionInfoProto();
    }

    public static UserSessionInfoProto getDefaultInstance() {
        return UserSessionInfoProto.DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<UserSessionInfoProto> PARSER = new com.google.protobuf.AbstractParser<UserSessionInfoProto>() {
        @Override
        public UserSessionInfoProto parsePartialFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
            return new UserSessionInfoProto(input, extensionRegistry);
        }
    };

    public static com.google.protobuf.Parser<UserSessionInfoProto> parser() {
        return UserSessionInfoProto.PARSER;
    }

    @Override
    public com.google.protobuf.Parser<UserSessionInfoProto> getParserForType() {
        return UserSessionInfoProto.PARSER;
    }

    @Override
    public UserSessionInfoProto getDefaultInstanceForType() {
        return UserSessionInfoProto.DEFAULT_INSTANCE;
    }
}
