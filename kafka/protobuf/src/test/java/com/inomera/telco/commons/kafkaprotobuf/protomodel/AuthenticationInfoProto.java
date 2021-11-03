package com.inomera.telco.commons.kafkaprotobuf.protomodel;

/**
 * Protobuf type {@code com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto}
 */
// @@protoc_insertion_point(message_implements:com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto)
public final class AuthenticationInfoProto extends com.google.protobuf.GeneratedMessageV3 implements com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProtoOrBuilder {
    private static final long serialVersionUID = 0L;

    // Use AuthenticationInfoProto.newBuilder() to construct.
    private AuthenticationInfoProto(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
        super(builder);
    }

    private AuthenticationInfoProto() {
        msisdn_ = "";
        ssoUniqueId_ = "";
        ssoUsername_ = "";
        mtsCustomerId_ = "";
        adslNo_ = "";
        userType_ = "";
        token_ = "";
        firstName_ = "";
        lastName_ = "";
        serviceMessage_ = "";
    }

    @Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
        return this.unknownFields;
    }

    private AuthenticationInfoProto(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
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
                    case 0:
                        done = true;
                        break;
                    case 8: {
                        result_ = input.readInt32();
                        break;
                    }
                    case 16: {
                        fromAvea_ = input.readBool();
                        break;
                    }
                    case 26: {
                        String s = input.readStringRequireUtf8();
                        msisdn_ = s;
                        break;
                    }
                    case 34: {
                        String s = input.readStringRequireUtf8();
                        ssoUniqueId_ = s;
                        break;
                    }
                    case 42: {
                        String s = input.readStringRequireUtf8();
                        ssoUsername_ = s;
                        break;
                    }
                    case 50: {
                        String s = input.readStringRequireUtf8();
                        mtsCustomerId_ = s;
                        break;
                    }
                    case 58: {
                        String s = input.readStringRequireUtf8();
                        adslNo_ = s;
                        break;
                    }
                    case 64: {
                        fromSSO_ = input.readBool();
                        break;
                    }
                    case 72: {
                        fromSSOMsisdn_ = input.readBool();
                        break;
                    }
                    case 82: {
                        String s = input.readStringRequireUtf8();
                        userType_ = s;
                        break;
                    }
                    case 90: {
                        String s = input.readStringRequireUtf8();
                        token_ = s;
                        break;
                    }
                    case 98: {
                        String s = input.readStringRequireUtf8();
                        firstName_ = s;
                        break;
                    }
                    case 106: {
                        String s = input.readStringRequireUtf8();
                        lastName_ = s;
                        break;
                    }
                    case 114: {
                        String s = input.readStringRequireUtf8();
                        serviceMessage_ = s;
                        break;
                    }
                    case 120: {
                        premium_ = input.readBool();
                        break;
                    }
                    case 128: {
                        fromMobileConnect_ = input.readBool();
                        break;
                    }
                    default: {
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
        return com.inomera.telco.commons.kafkaprotobuf.protomodel.UserSessionInfoProtoOuterClass.internal_static_com_inomera_muud_domain_session_AuthenticationInfoProto_descriptor;
    }

    @Override
    protected FieldAccessorTable internalGetFieldAccessorTable() {
        return com.inomera.telco.commons.kafkaprotobuf.protomodel.UserSessionInfoProtoOuterClass.internal_static_com_inomera_muud_domain_session_AuthenticationInfoProto_fieldAccessorTable.ensureFieldAccessorsInitialized(AuthenticationInfoProto.class, Builder.class);
    }

    public static final int RESULT_FIELD_NUMBER = 1;

    private int result_;

    /**
     * <code>int32 result = 1;</code>
     */
    public int getResult() {
        return result_;
    }

    public static final int FROMAVEA_FIELD_NUMBER = 2;

    private boolean fromAvea_;

    /**
     * <code>bool fromAvea = 2;</code>
     */
    public boolean getFromAvea() {
        return fromAvea_;
    }

    public static final int MSISDN_FIELD_NUMBER = 3;

    private volatile Object msisdn_;

    /**
     * <code>string msisdn = 3;</code>
     */
    public String getMsisdn() {
        Object ref = msisdn_;
        if (ref instanceof String) {
            return ((String) (ref));
        } else {
            com.google.protobuf.ByteString bs = ((com.google.protobuf.ByteString) (ref));
            String s = bs.toStringUtf8();
            msisdn_ = s;
            return s;
        }
    }

    /**
     * <code>string msisdn = 3;</code>
     */
    public com.google.protobuf.ByteString getMsisdnBytes() {
        Object ref = msisdn_;
        if (ref instanceof String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8(((String) (ref)));
            msisdn_ = b;
            return b;
        } else {
            return ((com.google.protobuf.ByteString) (ref));
        }
    }

    public static final int SSOUNIQUEID_FIELD_NUMBER = 4;

    private volatile Object ssoUniqueId_;

    /**
     * <code>string ssoUniqueId = 4;</code>
     */
    public String getSsoUniqueId() {
        Object ref = ssoUniqueId_;
        if (ref instanceof String) {
            return ((String) (ref));
        } else {
            com.google.protobuf.ByteString bs = ((com.google.protobuf.ByteString) (ref));
            String s = bs.toStringUtf8();
            ssoUniqueId_ = s;
            return s;
        }
    }

    /**
     * <code>string ssoUniqueId = 4;</code>
     */
    public com.google.protobuf.ByteString getSsoUniqueIdBytes() {
        Object ref = ssoUniqueId_;
        if (ref instanceof String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8(((String) (ref)));
            ssoUniqueId_ = b;
            return b;
        } else {
            return ((com.google.protobuf.ByteString) (ref));
        }
    }

    public static final int SSOUSERNAME_FIELD_NUMBER = 5;

    private volatile Object ssoUsername_;

    /**
     * <code>string ssoUsername = 5;</code>
     */
    public String getSsoUsername() {
        Object ref = ssoUsername_;
        if (ref instanceof String) {
            return ((String) (ref));
        } else {
            com.google.protobuf.ByteString bs = ((com.google.protobuf.ByteString) (ref));
            String s = bs.toStringUtf8();
            ssoUsername_ = s;
            return s;
        }
    }

    /**
     * <code>string ssoUsername = 5;</code>
     */
    public com.google.protobuf.ByteString getSsoUsernameBytes() {
        Object ref = ssoUsername_;
        if (ref instanceof String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8(((String) (ref)));
            ssoUsername_ = b;
            return b;
        } else {
            return ((com.google.protobuf.ByteString) (ref));
        }
    }

    public static final int MTSCUSTOMERID_FIELD_NUMBER = 6;

    private volatile Object mtsCustomerId_;

    /**
     * <code>string mtsCustomerId = 6;</code>
     */
    public String getMtsCustomerId() {
        Object ref = mtsCustomerId_;
        if (ref instanceof String) {
            return ((String) (ref));
        } else {
            com.google.protobuf.ByteString bs = ((com.google.protobuf.ByteString) (ref));
            String s = bs.toStringUtf8();
            mtsCustomerId_ = s;
            return s;
        }
    }

    /**
     * <code>string mtsCustomerId = 6;</code>
     */
    public com.google.protobuf.ByteString getMtsCustomerIdBytes() {
        Object ref = mtsCustomerId_;
        if (ref instanceof String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8(((String) (ref)));
            mtsCustomerId_ = b;
            return b;
        } else {
            return ((com.google.protobuf.ByteString) (ref));
        }
    }

    public static final int ADSLNO_FIELD_NUMBER = 7;

    private volatile Object adslNo_;

    /**
     * <code>string adslNo = 7;</code>
     */
    public String getAdslNo() {
        Object ref = adslNo_;
        if (ref instanceof String) {
            return ((String) (ref));
        } else {
            com.google.protobuf.ByteString bs = ((com.google.protobuf.ByteString) (ref));
            String s = bs.toStringUtf8();
            adslNo_ = s;
            return s;
        }
    }

    /**
     * <code>string adslNo = 7;</code>
     */
    public com.google.protobuf.ByteString getAdslNoBytes() {
        Object ref = adslNo_;
        if (ref instanceof String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8(((String) (ref)));
            adslNo_ = b;
            return b;
        } else {
            return ((com.google.protobuf.ByteString) (ref));
        }
    }

    public static final int FROMSSO_FIELD_NUMBER = 8;

    private boolean fromSSO_;

    /**
     * <code>bool fromSSO = 8;</code>
     */
    public boolean getFromSSO() {
        return fromSSO_;
    }

    public static final int FROMSSOMSISDN_FIELD_NUMBER = 9;

    private boolean fromSSOMsisdn_;

    /**
     * <code>bool fromSSOMsisdn = 9;</code>
     */
    public boolean getFromSSOMsisdn() {
        return fromSSOMsisdn_;
    }

    public static final int USERTYPE_FIELD_NUMBER = 10;

    private volatile Object userType_;

    /**
     * <code>string userType = 10;</code>
     */
    public String getUserType() {
        Object ref = userType_;
        if (ref instanceof String) {
            return ((String) (ref));
        } else {
            com.google.protobuf.ByteString bs = ((com.google.protobuf.ByteString) (ref));
            String s = bs.toStringUtf8();
            userType_ = s;
            return s;
        }
    }

    /**
     * <code>string userType = 10;</code>
     */
    public com.google.protobuf.ByteString getUserTypeBytes() {
        Object ref = userType_;
        if (ref instanceof String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8(((String) (ref)));
            userType_ = b;
            return b;
        } else {
            return ((com.google.protobuf.ByteString) (ref));
        }
    }

    public static final int TOKEN_FIELD_NUMBER = 11;

    private volatile Object token_;

    /**
     * <code>string token = 11;</code>
     */
    public String getToken() {
        Object ref = token_;
        if (ref instanceof String) {
            return ((String) (ref));
        } else {
            com.google.protobuf.ByteString bs = ((com.google.protobuf.ByteString) (ref));
            String s = bs.toStringUtf8();
            token_ = s;
            return s;
        }
    }

    /**
     * <code>string token = 11;</code>
     */
    public com.google.protobuf.ByteString getTokenBytes() {
        Object ref = token_;
        if (ref instanceof String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8(((String) (ref)));
            token_ = b;
            return b;
        } else {
            return ((com.google.protobuf.ByteString) (ref));
        }
    }

    public static final int FIRSTNAME_FIELD_NUMBER = 12;

    private volatile Object firstName_;

    /**
     * <code>string firstName = 12;</code>
     */
    public String getFirstName() {
        Object ref = firstName_;
        if (ref instanceof String) {
            return ((String) (ref));
        } else {
            com.google.protobuf.ByteString bs = ((com.google.protobuf.ByteString) (ref));
            String s = bs.toStringUtf8();
            firstName_ = s;
            return s;
        }
    }

    /**
     * <code>string firstName = 12;</code>
     */
    public com.google.protobuf.ByteString getFirstNameBytes() {
        Object ref = firstName_;
        if (ref instanceof String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8(((String) (ref)));
            firstName_ = b;
            return b;
        } else {
            return ((com.google.protobuf.ByteString) (ref));
        }
    }

    public static final int LASTNAME_FIELD_NUMBER = 13;

    private volatile Object lastName_;

    /**
     * <code>string lastName = 13;</code>
     */
    public String getLastName() {
        Object ref = lastName_;
        if (ref instanceof String) {
            return ((String) (ref));
        } else {
            com.google.protobuf.ByteString bs = ((com.google.protobuf.ByteString) (ref));
            String s = bs.toStringUtf8();
            lastName_ = s;
            return s;
        }
    }

    /**
     * <code>string lastName = 13;</code>
     */
    public com.google.protobuf.ByteString getLastNameBytes() {
        Object ref = lastName_;
        if (ref instanceof String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8(((String) (ref)));
            lastName_ = b;
            return b;
        } else {
            return ((com.google.protobuf.ByteString) (ref));
        }
    }

    public static final int SERVICEMESSAGE_FIELD_NUMBER = 14;

    private volatile Object serviceMessage_;

    /**
     * <code>string serviceMessage = 14;</code>
     */
    public String getServiceMessage() {
        Object ref = serviceMessage_;
        if (ref instanceof String) {
            return ((String) (ref));
        } else {
            com.google.protobuf.ByteString bs = ((com.google.protobuf.ByteString) (ref));
            String s = bs.toStringUtf8();
            serviceMessage_ = s;
            return s;
        }
    }

    /**
     * <code>string serviceMessage = 14;</code>
     */
    public com.google.protobuf.ByteString getServiceMessageBytes() {
        Object ref = serviceMessage_;
        if (ref instanceof String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8(((String) (ref)));
            serviceMessage_ = b;
            return b;
        } else {
            return ((com.google.protobuf.ByteString) (ref));
        }
    }

    public static final int PREMIUM_FIELD_NUMBER = 15;

    private boolean premium_;

    /**
     * <code>bool premium = 15;</code>
     */
    public boolean getPremium() {
        return premium_;
    }

    public static final int FROMMOBILECONNECT_FIELD_NUMBER = 16;

    private boolean fromMobileConnect_;

    /**
     * <code>bool fromMobileConnect = 16;</code>
     */
    public boolean getFromMobileConnect() {
        return fromMobileConnect_;
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
        if (result_ != 0) {
            output.writeInt32(1, result_);
        }
        if (fromAvea_ != false) {
            output.writeBool(2, fromAvea_);
        }
        if (!getMsisdnBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 3, msisdn_);
        }
        if (!getSsoUniqueIdBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 4, ssoUniqueId_);
        }
        if (!getSsoUsernameBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 5, ssoUsername_);
        }
        if (!getMtsCustomerIdBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 6, mtsCustomerId_);
        }
        if (!getAdslNoBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 7, adslNo_);
        }
        if (fromSSO_ != false) {
            output.writeBool(8, fromSSO_);
        }
        if (fromSSOMsisdn_ != false) {
            output.writeBool(9, fromSSOMsisdn_);
        }
        if (!getUserTypeBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 10, userType_);
        }
        if (!getTokenBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 11, token_);
        }
        if (!getFirstNameBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 12, firstName_);
        }
        if (!getLastNameBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 13, lastName_);
        }
        if (!getServiceMessageBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 14, serviceMessage_);
        }
        if (premium_ != false) {
            output.writeBool(15, premium_);
        }
        if (fromMobileConnect_ != false) {
            output.writeBool(16, fromMobileConnect_);
        }
        unknownFields.writeTo(output);
    }

    @Override
    public int getSerializedSize() {
        int size = memoizedSize;
        if (size != (-1))
            return size;

        size = 0;
        if (result_ != 0) {
            size += com.google.protobuf.CodedOutputStream.computeInt32Size(1, result_);
        }
        if (fromAvea_ != false) {
            size += com.google.protobuf.CodedOutputStream.computeBoolSize(2, fromAvea_);
        }
        if (!getMsisdnBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(3, msisdn_);
        }
        if (!getSsoUniqueIdBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(4, ssoUniqueId_);
        }
        if (!getSsoUsernameBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(5, ssoUsername_);
        }
        if (!getMtsCustomerIdBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(6, mtsCustomerId_);
        }
        if (!getAdslNoBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(7, adslNo_);
        }
        if (fromSSO_ != false) {
            size += com.google.protobuf.CodedOutputStream.computeBoolSize(8, fromSSO_);
        }
        if (fromSSOMsisdn_ != false) {
            size += com.google.protobuf.CodedOutputStream.computeBoolSize(9, fromSSOMsisdn_);
        }
        if (!getUserTypeBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(10, userType_);
        }
        if (!getTokenBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(11, token_);
        }
        if (!getFirstNameBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(12, firstName_);
        }
        if (!getLastNameBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(13, lastName_);
        }
        if (!getServiceMessageBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(14, serviceMessage_);
        }
        if (premium_ != false) {
            size += com.google.protobuf.CodedOutputStream.computeBoolSize(15, premium_);
        }
        if (fromMobileConnect_ != false) {
            size += com.google.protobuf.CodedOutputStream.computeBoolSize(16, fromMobileConnect_);
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
        if (!(obj instanceof AuthenticationInfoProto)) {
            return super.equals(obj);
        }
        AuthenticationInfoProto other = ((AuthenticationInfoProto) (obj));
        if (getResult() != other.getResult())
            return false;

        if (getFromAvea() != other.getFromAvea())
            return false;

        if (!getMsisdn().equals(other.getMsisdn()))
            return false;

        if (!getSsoUniqueId().equals(other.getSsoUniqueId()))
            return false;

        if (!getSsoUsername().equals(other.getSsoUsername()))
            return false;

        if (!getMtsCustomerId().equals(other.getMtsCustomerId()))
            return false;

        if (!getAdslNo().equals(other.getAdslNo()))
            return false;

        if (getFromSSO() != other.getFromSSO())
            return false;

        if (getFromSSOMsisdn() != other.getFromSSOMsisdn())
            return false;

        if (!getUserType().equals(other.getUserType()))
            return false;

        if (!getToken().equals(other.getToken()))
            return false;

        if (!getFirstName().equals(other.getFirstName()))
            return false;

        if (!getLastName().equals(other.getLastName()))
            return false;

        if (!getServiceMessage().equals(other.getServiceMessage()))
            return false;

        if (getPremium() != other.getPremium())
            return false;

        if (getFromMobileConnect() != other.getFromMobileConnect())
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
        hash = (19 * hash) + AuthenticationInfoProto.getDescriptor().hashCode();
        hash = (37 * hash) + AuthenticationInfoProto.RESULT_FIELD_NUMBER;
        hash = (53 * hash) + getResult();
        hash = (37 * hash) + AuthenticationInfoProto.FROMAVEA_FIELD_NUMBER;
        hash = (53 * hash) + com.google.protobuf.Internal.hashBoolean(getFromAvea());
        hash = (37 * hash) + AuthenticationInfoProto.MSISDN_FIELD_NUMBER;
        hash = (53 * hash) + getMsisdn().hashCode();
        hash = (37 * hash) + AuthenticationInfoProto.SSOUNIQUEID_FIELD_NUMBER;
        hash = (53 * hash) + getSsoUniqueId().hashCode();
        hash = (37 * hash) + AuthenticationInfoProto.SSOUSERNAME_FIELD_NUMBER;
        hash = (53 * hash) + getSsoUsername().hashCode();
        hash = (37 * hash) + AuthenticationInfoProto.MTSCUSTOMERID_FIELD_NUMBER;
        hash = (53 * hash) + getMtsCustomerId().hashCode();
        hash = (37 * hash) + AuthenticationInfoProto.ADSLNO_FIELD_NUMBER;
        hash = (53 * hash) + getAdslNo().hashCode();
        hash = (37 * hash) + AuthenticationInfoProto.FROMSSO_FIELD_NUMBER;
        hash = (53 * hash) + com.google.protobuf.Internal.hashBoolean(getFromSSO());
        hash = (37 * hash) + AuthenticationInfoProto.FROMSSOMSISDN_FIELD_NUMBER;
        hash = (53 * hash) + com.google.protobuf.Internal.hashBoolean(getFromSSOMsisdn());
        hash = (37 * hash) + AuthenticationInfoProto.USERTYPE_FIELD_NUMBER;
        hash = (53 * hash) + getUserType().hashCode();
        hash = (37 * hash) + AuthenticationInfoProto.TOKEN_FIELD_NUMBER;
        hash = (53 * hash) + getToken().hashCode();
        hash = (37 * hash) + AuthenticationInfoProto.FIRSTNAME_FIELD_NUMBER;
        hash = (53 * hash) + getFirstName().hashCode();
        hash = (37 * hash) + AuthenticationInfoProto.LASTNAME_FIELD_NUMBER;
        hash = (53 * hash) + getLastName().hashCode();
        hash = (37 * hash) + AuthenticationInfoProto.SERVICEMESSAGE_FIELD_NUMBER;
        hash = (53 * hash) + getServiceMessage().hashCode();
        hash = (37 * hash) + AuthenticationInfoProto.PREMIUM_FIELD_NUMBER;
        hash = (53 * hash) + com.google.protobuf.Internal.hashBoolean(getPremium());
        hash = (37 * hash) + AuthenticationInfoProto.FROMMOBILECONNECT_FIELD_NUMBER;
        hash = (53 * hash) + com.google.protobuf.Internal.hashBoolean(getFromMobileConnect());
        hash = (29 * hash) + unknownFields.hashCode();
        memoizedHashCode = hash;
        return hash;
    }

    public static AuthenticationInfoProto parseFrom(java.nio.ByteBuffer data) throws com.google.protobuf.InvalidProtocolBufferException {
        return AuthenticationInfoProto.PARSER.parseFrom(data);
    }

    public static AuthenticationInfoProto parseFrom(java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return AuthenticationInfoProto.PARSER.parseFrom(data, extensionRegistry);
    }

    public static AuthenticationInfoProto parseFrom(com.google.protobuf.ByteString data) throws com.google.protobuf.InvalidProtocolBufferException {
        return AuthenticationInfoProto.PARSER.parseFrom(data);
    }

    public static AuthenticationInfoProto parseFrom(com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return AuthenticationInfoProto.PARSER.parseFrom(data, extensionRegistry);
    }

    public static AuthenticationInfoProto parseFrom(byte[] data) throws com.google.protobuf.InvalidProtocolBufferException {
        return AuthenticationInfoProto.PARSER.parseFrom(data);
    }

    public static AuthenticationInfoProto parseFrom(byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return AuthenticationInfoProto.PARSER.parseFrom(data, extensionRegistry);
    }

    public static AuthenticationInfoProto parseFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(AuthenticationInfoProto.PARSER, input);
    }

    public static AuthenticationInfoProto parseFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(AuthenticationInfoProto.PARSER, input, extensionRegistry);
    }

    public static AuthenticationInfoProto parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(AuthenticationInfoProto.PARSER, input);
    }

    public static AuthenticationInfoProto parseDelimitedFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(AuthenticationInfoProto.PARSER, input, extensionRegistry);
    }

    public static AuthenticationInfoProto parseFrom(com.google.protobuf.CodedInputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(AuthenticationInfoProto.PARSER, input);
    }

    public static AuthenticationInfoProto parseFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(AuthenticationInfoProto.PARSER, input, extensionRegistry);
    }

    @Override
    public Builder newBuilderForType() {
        return AuthenticationInfoProto.newBuilder();
    }

    public static Builder newBuilder() {
        return AuthenticationInfoProto.DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(AuthenticationInfoProto prototype) {
        return AuthenticationInfoProto.DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    @Override
    public Builder toBuilder() {
        return this == AuthenticationInfoProto.DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(BuilderParent parent) {
        Builder builder = new Builder(parent);
        return builder;
    }

    /**
     * Protobuf type {@code com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto}
     */
    // @@protoc_insertion_point(builder_implements:com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto)
    // @@protoc_insertion_point(builder_scope:com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto)
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProtoOrBuilder {
        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return com.inomera.telco.commons.kafkaprotobuf.protomodel.UserSessionInfoProtoOuterClass.internal_static_com_inomera_muud_domain_session_AuthenticationInfoProto_descriptor;
        }

        @Override
        protected FieldAccessorTable internalGetFieldAccessorTable() {
            return com.inomera.telco.commons.kafkaprotobuf.protomodel.UserSessionInfoProtoOuterClass.internal_static_com_inomera_muud_domain_session_AuthenticationInfoProto_fieldAccessorTable.ensureFieldAccessorsInitialized(AuthenticationInfoProto.class, Builder.class);
        }

        // Construct using com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto.newBuilder()
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
            result_ = 0;
            fromAvea_ = false;
            msisdn_ = "";
            ssoUniqueId_ = "";
            ssoUsername_ = "";
            mtsCustomerId_ = "";
            adslNo_ = "";
            fromSSO_ = false;
            fromSSOMsisdn_ = false;
            userType_ = "";
            token_ = "";
            firstName_ = "";
            lastName_ = "";
            serviceMessage_ = "";
            premium_ = false;
            fromMobileConnect_ = false;
            return this;
        }

        @Override
        public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
            return com.inomera.telco.commons.kafkaprotobuf.protomodel.UserSessionInfoProtoOuterClass.internal_static_com_inomera_muud_domain_session_AuthenticationInfoProto_descriptor;
        }

        @Override
        public AuthenticationInfoProto getDefaultInstanceForType() {
            return AuthenticationInfoProto.getDefaultInstance();
        }

        @Override
        public AuthenticationInfoProto build() {
            AuthenticationInfoProto result = buildPartial();
            if (!result.isInitialized()) {
                throw com.google.protobuf.AbstractMessage.Builder.newUninitializedMessageException(result);
            }
            return result;
        }

        @Override
        public AuthenticationInfoProto buildPartial() {
            AuthenticationInfoProto result = new AuthenticationInfoProto(this);
            result.result_ = result_;
            result.fromAvea_ = fromAvea_;
            result.msisdn_ = msisdn_;
            result.ssoUniqueId_ = ssoUniqueId_;
            result.ssoUsername_ = ssoUsername_;
            result.mtsCustomerId_ = mtsCustomerId_;
            result.adslNo_ = adslNo_;
            result.fromSSO_ = fromSSO_;
            result.fromSSOMsisdn_ = fromSSOMsisdn_;
            result.userType_ = userType_;
            result.token_ = token_;
            result.firstName_ = firstName_;
            result.lastName_ = lastName_;
            result.serviceMessage_ = serviceMessage_;
            result.premium_ = premium_;
            result.fromMobileConnect_ = fromMobileConnect_;
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
            if (other instanceof AuthenticationInfoProto) {
                return mergeFrom(((AuthenticationInfoProto) (other)));
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(AuthenticationInfoProto other) {
            if (other == AuthenticationInfoProto.getDefaultInstance())
                return this;

            if (other.getResult() != 0) {
                setResult(other.getResult());
            }
            if (other.getFromAvea() != false) {
                setFromAvea(other.getFromAvea());
            }
            if (!other.getMsisdn().isEmpty()) {
                msisdn_ = other.msisdn_;
                onChanged();
            }
            if (!other.getSsoUniqueId().isEmpty()) {
                ssoUniqueId_ = other.ssoUniqueId_;
                onChanged();
            }
            if (!other.getSsoUsername().isEmpty()) {
                ssoUsername_ = other.ssoUsername_;
                onChanged();
            }
            if (!other.getMtsCustomerId().isEmpty()) {
                mtsCustomerId_ = other.mtsCustomerId_;
                onChanged();
            }
            if (!other.getAdslNo().isEmpty()) {
                adslNo_ = other.adslNo_;
                onChanged();
            }
            if (other.getFromSSO() != false) {
                setFromSSO(other.getFromSSO());
            }
            if (other.getFromSSOMsisdn() != false) {
                setFromSSOMsisdn(other.getFromSSOMsisdn());
            }
            if (!other.getUserType().isEmpty()) {
                userType_ = other.userType_;
                onChanged();
            }
            if (!other.getToken().isEmpty()) {
                token_ = other.token_;
                onChanged();
            }
            if (!other.getFirstName().isEmpty()) {
                firstName_ = other.firstName_;
                onChanged();
            }
            if (!other.getLastName().isEmpty()) {
                lastName_ = other.lastName_;
                onChanged();
            }
            if (!other.getServiceMessage().isEmpty()) {
                serviceMessage_ = other.serviceMessage_;
                onChanged();
            }
            if (other.getPremium() != false) {
                setPremium(other.getPremium());
            }
            if (other.getFromMobileConnect() != false) {
                setFromMobileConnect(other.getFromMobileConnect());
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
            AuthenticationInfoProto parsedMessage = null;
            try {
                parsedMessage = AuthenticationInfoProto.PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                parsedMessage = ((AuthenticationInfoProto) (e.getUnfinishedMessage()));
                throw e.unwrapIOException();
            } finally {
                if (parsedMessage != null) {
                    mergeFrom(parsedMessage);
                }
            }
            return this;
        }

        private int result_;

        /**
         * <code>int32 result = 1;</code>
         */
        public int getResult() {
            return result_;
        }

        /**
         * <code>int32 result = 1;</code>
         */
        public Builder setResult(int value) {
            result_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>int32 result = 1;</code>
         */
        public Builder clearResult() {
            result_ = 0;
            onChanged();
            return this;
        }

        private boolean fromAvea_;

        /**
         * <code>bool fromAvea = 2;</code>
         */
        public boolean getFromAvea() {
            return fromAvea_;
        }

        /**
         * <code>bool fromAvea = 2;</code>
         */
        public Builder setFromAvea(boolean value) {
            fromAvea_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>bool fromAvea = 2;</code>
         */
        public Builder clearFromAvea() {
            fromAvea_ = false;
            onChanged();
            return this;
        }

        private Object msisdn_ = "";

        /**
         * <code>string msisdn = 3;</code>
         */
        public String getMsisdn() {
            Object ref = msisdn_;
            if (!(ref instanceof String)) {
                com.google.protobuf.ByteString bs = ((com.google.protobuf.ByteString) (ref));
                String s = bs.toStringUtf8();
                msisdn_ = s;
                return s;
            } else {
                return ((String) (ref));
            }
        }

        /**
         * <code>string msisdn = 3;</code>
         */
        public com.google.protobuf.ByteString getMsisdnBytes() {
            Object ref = msisdn_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8(((String) (ref)));
                msisdn_ = b;
                return b;
            } else {
                return ((com.google.protobuf.ByteString) (ref));
            }
        }

        /**
         * <code>string msisdn = 3;</code>
         */
        public Builder setMsisdn(String value) {
            if (value == null) {
                return this;
            }
            ;
            if (value == null) {
                throw new NullPointerException();
            }
            msisdn_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>string msisdn = 3;</code>
         */
        public Builder clearMsisdn() {
            msisdn_ = AuthenticationInfoProto.getDefaultInstance().getMsisdn();
            onChanged();
            return this;
        }

        /**
         * <code>string msisdn = 3;</code>
         */
        public Builder setMsisdnBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                return this;
            }
            ;
            if (value == null) {
                throw new NullPointerException();
            }
            com.google.protobuf.AbstractMessageLite.checkByteStringIsUtf8(value);
            msisdn_ = value;
            onChanged();
            return this;
        }

        private Object ssoUniqueId_ = "";

        /**
         * <code>string ssoUniqueId = 4;</code>
         */
        public String getSsoUniqueId() {
            Object ref = ssoUniqueId_;
            if (!(ref instanceof String)) {
                com.google.protobuf.ByteString bs = ((com.google.protobuf.ByteString) (ref));
                String s = bs.toStringUtf8();
                ssoUniqueId_ = s;
                return s;
            } else {
                return ((String) (ref));
            }
        }

        /**
         * <code>string ssoUniqueId = 4;</code>
         */
        public com.google.protobuf.ByteString getSsoUniqueIdBytes() {
            Object ref = ssoUniqueId_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8(((String) (ref)));
                ssoUniqueId_ = b;
                return b;
            } else {
                return ((com.google.protobuf.ByteString) (ref));
            }
        }

        /**
         * <code>string ssoUniqueId = 4;</code>
         */
        public Builder setSsoUniqueId(String value) {
            if (value == null) {
                return this;
            }
            ;
            if (value == null) {
                throw new NullPointerException();
            }
            ssoUniqueId_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>string ssoUniqueId = 4;</code>
         */
        public Builder clearSsoUniqueId() {
            ssoUniqueId_ = AuthenticationInfoProto.getDefaultInstance().getSsoUniqueId();
            onChanged();
            return this;
        }

        /**
         * <code>string ssoUniqueId = 4;</code>
         */
        public Builder setSsoUniqueIdBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                return this;
            }
            ;
            if (value == null) {
                throw new NullPointerException();
            }
            com.google.protobuf.AbstractMessageLite.checkByteStringIsUtf8(value);
            ssoUniqueId_ = value;
            onChanged();
            return this;
        }

        private Object ssoUsername_ = "";

        /**
         * <code>string ssoUsername = 5;</code>
         */
        public String getSsoUsername() {
            Object ref = ssoUsername_;
            if (!(ref instanceof String)) {
                com.google.protobuf.ByteString bs = ((com.google.protobuf.ByteString) (ref));
                String s = bs.toStringUtf8();
                ssoUsername_ = s;
                return s;
            } else {
                return ((String) (ref));
            }
        }

        /**
         * <code>string ssoUsername = 5;</code>
         */
        public com.google.protobuf.ByteString getSsoUsernameBytes() {
            Object ref = ssoUsername_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8(((String) (ref)));
                ssoUsername_ = b;
                return b;
            } else {
                return ((com.google.protobuf.ByteString) (ref));
            }
        }

        /**
         * <code>string ssoUsername = 5;</code>
         */
        public Builder setSsoUsername(String value) {
            if (value == null) {
                return this;
            }
            ;
            if (value == null) {
                throw new NullPointerException();
            }
            ssoUsername_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>string ssoUsername = 5;</code>
         */
        public Builder clearSsoUsername() {
            ssoUsername_ = AuthenticationInfoProto.getDefaultInstance().getSsoUsername();
            onChanged();
            return this;
        }

        /**
         * <code>string ssoUsername = 5;</code>
         */
        public Builder setSsoUsernameBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                return this;
            }
            ;
            if (value == null) {
                throw new NullPointerException();
            }
            com.google.protobuf.AbstractMessageLite.checkByteStringIsUtf8(value);
            ssoUsername_ = value;
            onChanged();
            return this;
        }

        private Object mtsCustomerId_ = "";

        /**
         * <code>string mtsCustomerId = 6;</code>
         */
        public String getMtsCustomerId() {
            Object ref = mtsCustomerId_;
            if (!(ref instanceof String)) {
                com.google.protobuf.ByteString bs = ((com.google.protobuf.ByteString) (ref));
                String s = bs.toStringUtf8();
                mtsCustomerId_ = s;
                return s;
            } else {
                return ((String) (ref));
            }
        }

        /**
         * <code>string mtsCustomerId = 6;</code>
         */
        public com.google.protobuf.ByteString getMtsCustomerIdBytes() {
            Object ref = mtsCustomerId_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8(((String) (ref)));
                mtsCustomerId_ = b;
                return b;
            } else {
                return ((com.google.protobuf.ByteString) (ref));
            }
        }

        /**
         * <code>string mtsCustomerId = 6;</code>
         */
        public Builder setMtsCustomerId(String value) {
            if (value == null) {
                return this;
            }
            ;
            if (value == null) {
                throw new NullPointerException();
            }
            mtsCustomerId_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>string mtsCustomerId = 6;</code>
         */
        public Builder clearMtsCustomerId() {
            mtsCustomerId_ = AuthenticationInfoProto.getDefaultInstance().getMtsCustomerId();
            onChanged();
            return this;
        }

        /**
         * <code>string mtsCustomerId = 6;</code>
         */
        public Builder setMtsCustomerIdBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                return this;
            }
            ;
            if (value == null) {
                throw new NullPointerException();
            }
            com.google.protobuf.AbstractMessageLite.checkByteStringIsUtf8(value);
            mtsCustomerId_ = value;
            onChanged();
            return this;
        }

        private Object adslNo_ = "";

        /**
         * <code>string adslNo = 7;</code>
         */
        public String getAdslNo() {
            Object ref = adslNo_;
            if (!(ref instanceof String)) {
                com.google.protobuf.ByteString bs = ((com.google.protobuf.ByteString) (ref));
                String s = bs.toStringUtf8();
                adslNo_ = s;
                return s;
            } else {
                return ((String) (ref));
            }
        }

        /**
         * <code>string adslNo = 7;</code>
         */
        public com.google.protobuf.ByteString getAdslNoBytes() {
            Object ref = adslNo_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8(((String) (ref)));
                adslNo_ = b;
                return b;
            } else {
                return ((com.google.protobuf.ByteString) (ref));
            }
        }

        /**
         * <code>string adslNo = 7;</code>
         */
        public Builder setAdslNo(String value) {
            if (value == null) {
                return this;
            }
            ;
            if (value == null) {
                throw new NullPointerException();
            }
            adslNo_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>string adslNo = 7;</code>
         */
        public Builder clearAdslNo() {
            adslNo_ = AuthenticationInfoProto.getDefaultInstance().getAdslNo();
            onChanged();
            return this;
        }

        /**
         * <code>string adslNo = 7;</code>
         */
        public Builder setAdslNoBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                return this;
            }
            ;
            if (value == null) {
                throw new NullPointerException();
            }
            com.google.protobuf.AbstractMessageLite.checkByteStringIsUtf8(value);
            adslNo_ = value;
            onChanged();
            return this;
        }

        private boolean fromSSO_;

        /**
         * <code>bool fromSSO = 8;</code>
         */
        public boolean getFromSSO() {
            return fromSSO_;
        }

        /**
         * <code>bool fromSSO = 8;</code>
         */
        public Builder setFromSSO(boolean value) {
            fromSSO_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>bool fromSSO = 8;</code>
         */
        public Builder clearFromSSO() {
            fromSSO_ = false;
            onChanged();
            return this;
        }

        private boolean fromSSOMsisdn_;

        /**
         * <code>bool fromSSOMsisdn = 9;</code>
         */
        public boolean getFromSSOMsisdn() {
            return fromSSOMsisdn_;
        }

        /**
         * <code>bool fromSSOMsisdn = 9;</code>
         */
        public Builder setFromSSOMsisdn(boolean value) {
            fromSSOMsisdn_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>bool fromSSOMsisdn = 9;</code>
         */
        public Builder clearFromSSOMsisdn() {
            fromSSOMsisdn_ = false;
            onChanged();
            return this;
        }

        private Object userType_ = "";

        /**
         * <code>string userType = 10;</code>
         */
        public String getUserType() {
            Object ref = userType_;
            if (!(ref instanceof String)) {
                com.google.protobuf.ByteString bs = ((com.google.protobuf.ByteString) (ref));
                String s = bs.toStringUtf8();
                userType_ = s;
                return s;
            } else {
                return ((String) (ref));
            }
        }

        /**
         * <code>string userType = 10;</code>
         */
        public com.google.protobuf.ByteString getUserTypeBytes() {
            Object ref = userType_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8(((String) (ref)));
                userType_ = b;
                return b;
            } else {
                return ((com.google.protobuf.ByteString) (ref));
            }
        }

        /**
         * <code>string userType = 10;</code>
         */
        public Builder setUserType(String value) {
            if (value == null) {
                return this;
            }
            ;
            if (value == null) {
                throw new NullPointerException();
            }
            userType_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>string userType = 10;</code>
         */
        public Builder clearUserType() {
            userType_ = AuthenticationInfoProto.getDefaultInstance().getUserType();
            onChanged();
            return this;
        }

        /**
         * <code>string userType = 10;</code>
         */
        public Builder setUserTypeBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                return this;
            }
            ;
            if (value == null) {
                throw new NullPointerException();
            }
            com.google.protobuf.AbstractMessageLite.checkByteStringIsUtf8(value);
            userType_ = value;
            onChanged();
            return this;
        }

        private Object token_ = "";

        /**
         * <code>string token = 11;</code>
         */
        public String getToken() {
            Object ref = token_;
            if (!(ref instanceof String)) {
                com.google.protobuf.ByteString bs = ((com.google.protobuf.ByteString) (ref));
                String s = bs.toStringUtf8();
                token_ = s;
                return s;
            } else {
                return ((String) (ref));
            }
        }

        /**
         * <code>string token = 11;</code>
         */
        public com.google.protobuf.ByteString getTokenBytes() {
            Object ref = token_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8(((String) (ref)));
                token_ = b;
                return b;
            } else {
                return ((com.google.protobuf.ByteString) (ref));
            }
        }

        /**
         * <code>string token = 11;</code>
         */
        public Builder setToken(String value) {
            if (value == null) {
                return this;
            }
            ;
            if (value == null) {
                throw new NullPointerException();
            }
            token_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>string token = 11;</code>
         */
        public Builder clearToken() {
            token_ = AuthenticationInfoProto.getDefaultInstance().getToken();
            onChanged();
            return this;
        }

        /**
         * <code>string token = 11;</code>
         */
        public Builder setTokenBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                return this;
            }
            ;
            if (value == null) {
                throw new NullPointerException();
            }
            com.google.protobuf.AbstractMessageLite.checkByteStringIsUtf8(value);
            token_ = value;
            onChanged();
            return this;
        }

        private Object firstName_ = "";

        /**
         * <code>string firstName = 12;</code>
         */
        public String getFirstName() {
            Object ref = firstName_;
            if (!(ref instanceof String)) {
                com.google.protobuf.ByteString bs = ((com.google.protobuf.ByteString) (ref));
                String s = bs.toStringUtf8();
                firstName_ = s;
                return s;
            } else {
                return ((String) (ref));
            }
        }

        /**
         * <code>string firstName = 12;</code>
         */
        public com.google.protobuf.ByteString getFirstNameBytes() {
            Object ref = firstName_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8(((String) (ref)));
                firstName_ = b;
                return b;
            } else {
                return ((com.google.protobuf.ByteString) (ref));
            }
        }

        /**
         * <code>string firstName = 12;</code>
         */
        public Builder setFirstName(String value) {
            if (value == null) {
                return this;
            }
            ;
            if (value == null) {
                throw new NullPointerException();
            }
            firstName_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>string firstName = 12;</code>
         */
        public Builder clearFirstName() {
            firstName_ = AuthenticationInfoProto.getDefaultInstance().getFirstName();
            onChanged();
            return this;
        }

        /**
         * <code>string firstName = 12;</code>
         */
        public Builder setFirstNameBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                return this;
            }
            ;
            if (value == null) {
                throw new NullPointerException();
            }
            com.google.protobuf.AbstractMessageLite.checkByteStringIsUtf8(value);
            firstName_ = value;
            onChanged();
            return this;
        }

        private Object lastName_ = "";

        /**
         * <code>string lastName = 13;</code>
         */
        public String getLastName() {
            Object ref = lastName_;
            if (!(ref instanceof String)) {
                com.google.protobuf.ByteString bs = ((com.google.protobuf.ByteString) (ref));
                String s = bs.toStringUtf8();
                lastName_ = s;
                return s;
            } else {
                return ((String) (ref));
            }
        }

        /**
         * <code>string lastName = 13;</code>
         */
        public com.google.protobuf.ByteString getLastNameBytes() {
            Object ref = lastName_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8(((String) (ref)));
                lastName_ = b;
                return b;
            } else {
                return ((com.google.protobuf.ByteString) (ref));
            }
        }

        /**
         * <code>string lastName = 13;</code>
         */
        public Builder setLastName(String value) {
            if (value == null) {
                return this;
            }
            ;
            if (value == null) {
                throw new NullPointerException();
            }
            lastName_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>string lastName = 13;</code>
         */
        public Builder clearLastName() {
            lastName_ = AuthenticationInfoProto.getDefaultInstance().getLastName();
            onChanged();
            return this;
        }

        /**
         * <code>string lastName = 13;</code>
         */
        public Builder setLastNameBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                return this;
            }
            ;
            if (value == null) {
                throw new NullPointerException();
            }
            com.google.protobuf.AbstractMessageLite.checkByteStringIsUtf8(value);
            lastName_ = value;
            onChanged();
            return this;
        }

        private Object serviceMessage_ = "";

        /**
         * <code>string serviceMessage = 14;</code>
         */
        public String getServiceMessage() {
            Object ref = serviceMessage_;
            if (!(ref instanceof String)) {
                com.google.protobuf.ByteString bs = ((com.google.protobuf.ByteString) (ref));
                String s = bs.toStringUtf8();
                serviceMessage_ = s;
                return s;
            } else {
                return ((String) (ref));
            }
        }

        /**
         * <code>string serviceMessage = 14;</code>
         */
        public com.google.protobuf.ByteString getServiceMessageBytes() {
            Object ref = serviceMessage_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8(((String) (ref)));
                serviceMessage_ = b;
                return b;
            } else {
                return ((com.google.protobuf.ByteString) (ref));
            }
        }

        /**
         * <code>string serviceMessage = 14;</code>
         */
        public Builder setServiceMessage(String value) {
            if (value == null) {
                return this;
            }
            ;
            if (value == null) {
                throw new NullPointerException();
            }
            serviceMessage_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>string serviceMessage = 14;</code>
         */
        public Builder clearServiceMessage() {
            serviceMessage_ = AuthenticationInfoProto.getDefaultInstance().getServiceMessage();
            onChanged();
            return this;
        }

        /**
         * <code>string serviceMessage = 14;</code>
         */
        public Builder setServiceMessageBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                return this;
            }
            ;
            if (value == null) {
                throw new NullPointerException();
            }
            com.google.protobuf.AbstractMessageLite.checkByteStringIsUtf8(value);
            serviceMessage_ = value;
            onChanged();
            return this;
        }

        private boolean premium_;

        /**
         * <code>bool premium = 15;</code>
         */
        public boolean getPremium() {
            return premium_;
        }

        /**
         * <code>bool premium = 15;</code>
         */
        public Builder setPremium(boolean value) {
            premium_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>bool premium = 15;</code>
         */
        public Builder clearPremium() {
            premium_ = false;
            onChanged();
            return this;
        }

        private boolean fromMobileConnect_;

        /**
         * <code>bool fromMobileConnect = 16;</code>
         */
        public boolean getFromMobileConnect() {
            return fromMobileConnect_;
        }

        /**
         * <code>bool fromMobileConnect = 16;</code>
         */
        public Builder setFromMobileConnect(boolean value) {
            fromMobileConnect_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>bool fromMobileConnect = 16;</code>
         */
        public Builder clearFromMobileConnect() {
            fromMobileConnect_ = false;
            onChanged();
            return this;
        }

        @Override
        public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
            if (unknownFields == null) {
                return this;
            }
            ;
            return super.setUnknownFields(unknownFields);
        }

        @Override
        public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
            return super.mergeUnknownFields(unknownFields);
        }
    }

    // @@protoc_insertion_point(class_scope:com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto)
    private static final AuthenticationInfoProto DEFAULT_INSTANCE;

    static {
        DEFAULT_INSTANCE = new AuthenticationInfoProto();
    }

    public static AuthenticationInfoProto getDefaultInstance() {
        return AuthenticationInfoProto.DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<AuthenticationInfoProto> PARSER = new com.google.protobuf.AbstractParser<AuthenticationInfoProto>() {
        @Override
        public AuthenticationInfoProto parsePartialFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
            return new AuthenticationInfoProto(input, extensionRegistry);
        }
    };

    public static com.google.protobuf.Parser<AuthenticationInfoProto> parser() {
        return AuthenticationInfoProto.PARSER;
    }

    @Override
    public com.google.protobuf.Parser<AuthenticationInfoProto> getParserForType() {
        return AuthenticationInfoProto.PARSER;
    }

    @Override
    public AuthenticationInfoProto getDefaultInstanceForType() {
        return AuthenticationInfoProto.DEFAULT_INSTANCE;
    }
}
